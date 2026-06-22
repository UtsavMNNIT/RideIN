package com.rideflow.matching.application.port.out;

import java.util.UUID;

/** AutoCloseable handle for a per-driver dispatch lock; {@link #close()} releases it if held. */
public interface DriverLock extends AutoCloseable {

    /** The driver this lock pertains to. */
    UUID driverId();

    /** Whether the lock was actually acquired (false ⇒ contended elsewhere). */
    boolean isAcquired();

    /** Release the lock if held; overridden to not declare checked exceptions. */
    @Override
    void close();
}
