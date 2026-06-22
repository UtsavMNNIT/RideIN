package com.rideflow.matching.application.port.out;

import java.util.UUID;

/** Output port for acquiring a per-driver distributed lock during dispatch. */
public interface DriverLockService {

    /**
     * Attempt to acquire the lock for {@code driverId}, returning a handle whose
     * {@link DriverLock#isAcquired()} reflects whether exclusion was won.
     * Contention returns an unheld handle; hard lock-infra failure throws
     * {@link LockAcquisitionException}.
     */
    DriverLock tryAcquire(UUID driverId);
}
