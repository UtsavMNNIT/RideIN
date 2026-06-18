package com.rideflow.matching.infrastructure.cache.redis;

import com.rideflow.matching.application.port.out.DriverLock;
import com.rideflow.matching.application.port.out.DriverLockService;
import com.rideflow.matching.application.port.out.LockAcquisitionException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redisson-backed implementation of {@link DriverLockService}.
 *
 * <h2>Design decisions (recap — see PR description for full reasoning)</h2>
 * <ul>
 *   <li><b>Watchdog OFF.</b> We always pass an explicit {@code leaseTime};
 *       Redisson's auto-renewal activates only when {@code leaseTime == -1}.
 *       If the dispatcher JVM hangs or stop-the-worlds past the lease, we
 *       <em>want</em> the lock to expire so another replica can dispatch.</li>
 *   <li><b>{@code waitTime == 0}.</b> Dispatch contention is resolved by
 *       iterating to the next-best candidate in score order, not by queuing
 *       behind the current holder. Queuing inflates p99 dispatch latency.</li>
 *   <li><b>Owner-checked release.</b> Redisson encodes
 *       {@code (connectionId, threadId)} in the lock value and rejects
 *       cross-thread {@code unlock()} atomically — eliminates the
 *       "release someone else's lock" footgun of raw {@code SETNX + DEL}.</li>
 *   <li><b>Fail closed.</b> Hard Redis failures surface as
 *       {@link LockAcquisitionException}; the use case treats this as a
 *       dispatch ERROR and NACKs the Kafka message. Never assign a driver
 *       without confirmed exclusion.</li>
 * </ul>
 *
 * <h2>Single-Redis caveat</h2>
 * Backed by one Redis instance, not Redlock across N independent masters.
 * Lock loss during a Redis restart is possible in principle; the DB-side
 * partial unique index on {@code rides(assigned_driver_id) WHERE
 * status='ASSIGNED'} is the authoritative second line of defence. To go
 * multi-master, replace this class with a {@code RedissonRedLock}-backed
 * adapter — the port contract is unchanged.
 */
@Component
public class RedissonDriverLockService implements DriverLockService {

    private static final Logger log = LoggerFactory.getLogger(RedissonDriverLockService.class);

    private final RedissonClient redisson;
    private final String         keyPrefix;
    private final long           waitMillis;
    private final long           leaseMillis;

    private final Counter acquiredCounter;
    private final Counter contendedCounter;
    private final Counter failedCounter;
    private final Timer   acquireTimer;

    public RedissonDriverLockService(
            RedissonClient redisson,
            MeterRegistry meterRegistry,
            @Value("${rideflow.dispatch.lock.key-prefix}") String keyPrefix,
            @Value("${rideflow.dispatch.lock.wait-millis}") long waitMillis,
            @Value("${rideflow.dispatch.lock.lease-millis}") long leaseMillis) {

        this.redisson    = redisson;
        this.keyPrefix   = keyPrefix;
        this.waitMillis  = waitMillis;
        this.leaseMillis = leaseMillis;

        this.acquiredCounter  = meterRegistry.counter("dispatch.lock.acquire", "result", "acquired");
        this.contendedCounter = meterRegistry.counter("dispatch.lock.acquire", "result", "contended");
        this.failedCounter    = meterRegistry.counter("dispatch.lock.acquire", "result", "failed");
        this.acquireTimer     = meterRegistry.timer("dispatch.lock.acquire.duration");
    }

    @Override
    public DriverLock tryAcquire(UUID driverId) {
        if (driverId == null) {
            throw new IllegalArgumentException("driverId is null");
        }

        final String key  = keyPrefix + driverId;
        final RLock  lock = redisson.getLock(key);

        final long startNanos = System.nanoTime();
        final boolean acquired;
        try {
            acquired = lock.tryLock(waitMillis, leaseMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failedCounter.increment();
            throw new LockAcquisitionException("Interrupted acquiring lock " + key, e);
        } catch (RuntimeException e) {
            // Redisson wraps connection-pool exhaustion, codec faults, and
            // Redis-down conditions as RuntimeExceptions. We fail closed so
            // the dispatcher NEVER assigns without confirmed exclusion.
            failedCounter.increment();
            throw new LockAcquisitionException("Redis failure acquiring lock " + key, e);
        } finally {
            acquireTimer.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        }

        if (acquired) {
            acquiredCounter.increment();
            log.debug("Lock acquired driverId={} leaseMs={}", driverId, leaseMillis);
            return new HeldLock(driverId, lock);
        }

        contendedCounter.increment();
        log.debug("Lock contended driverId={} — caller will try next candidate", driverId);
        return new UnheldLock(driverId);
    }

    // ------------------------------------------------------------------
    // Handle implementations
    // ------------------------------------------------------------------

    /**
     * Held-handle. Releases on {@link #close()} provided the calling thread
     * is still the owner (Redisson enforces this atomically).
     */
    private static final class HeldLock implements DriverLock {

        private final UUID    driverId;
        private final RLock   lock;
        private boolean       closed = false;

        HeldLock(UUID driverId, RLock lock) {
            this.driverId = driverId;
            this.lock     = lock;
        }

        @Override public UUID    driverId()   { return driverId; }
        @Override public boolean isAcquired() { return true; }

        @Override
        public void close() {
            if (closed) return;
            closed = true;

            try {
                // isHeldByCurrentThread() guards against:
                //   (a) the lease having already expired (TTL fired)
                //   (b) accidental cross-thread release
                // In either case unlock() would throw IllegalMonitorStateException;
                // we'd rather check than catch.
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                } else {
                    log.warn("Lock no longer held by current thread at close — "
                           + "lease likely expired driverId={} key={}",
                           driverId, lock.getName());
                }
            } catch (RuntimeException e) {
                // We MUST swallow: throwing from close() inside a
                // try-with-resources would mask any in-flight business
                // exception. The lock's TTL guarantees release within
                // leaseMillis even if this branch fires.
                log.warn("Failed to release lock driverId={} — TTL will reclaim", driverId, e);
            }
        }
    }

    /** Unheld handle — represents lock contention, not failure. */
    private record UnheldLock(UUID driverId) implements DriverLock {
        @Override public boolean isAcquired() { return false; }
        @Override public void    close()      { /* no-op */ }
    }
}
