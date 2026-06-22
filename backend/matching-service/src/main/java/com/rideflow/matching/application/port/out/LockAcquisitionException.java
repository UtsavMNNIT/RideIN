package com.rideflow.matching.application.port.out;

/** Thrown when the per-driver lock cannot be acquired due to hard lock-infrastructure failure (fail closed). */
public class LockAcquisitionException extends RuntimeException {

    public LockAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
