package com.rideflow.matching.domain.exception;

/**
 * Base type for matching-service domain exceptions. Lets the REST advice and the
 * Kafka error handler branch on one family rather than a grab-bag of unrelated
 * runtime exceptions.
 */
public abstract class MatchingException extends RuntimeException {

    protected MatchingException(String message) {
        super(message);
    }

    protected MatchingException(String message, Throwable cause) {
        super(message, cause);
    }
}
