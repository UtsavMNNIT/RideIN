package com.rideflow.rider.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RideStatusTest {

    @Test
    void terminalStates() {
        assertThat(RideStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(RideStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(RideStatus.NO_DRIVERS_FOUND.isTerminal()).isTrue();
    }

    @Test
    void nonTerminalStates() {
        assertThat(RideStatus.REQUESTED.isTerminal()).isFalse();
        assertThat(RideStatus.ASSIGNED.isTerminal()).isFalse();
        assertThat(RideStatus.STARTED.isTerminal()).isFalse();
    }

    @Test
    void forwardRankOrdering() {
        assertThat(RideStatus.REQUESTED.rank()).isLessThan(RideStatus.ASSIGNED.rank());
        assertThat(RideStatus.ASSIGNED.rank()).isLessThan(RideStatus.STARTED.rank());
        assertThat(RideStatus.STARTED.rank()).isLessThan(RideStatus.COMPLETED.rank());
    }
}
