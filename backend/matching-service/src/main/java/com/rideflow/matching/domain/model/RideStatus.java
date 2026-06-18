package com.rideflow.matching.domain.model;

public enum RideStatus {
    REQUESTED,
    DISPATCHING,
    ASSIGNED,
    DISPATCH_FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == ASSIGNED || this == DISPATCH_FAILED || this == CANCELLED;
    }
}
