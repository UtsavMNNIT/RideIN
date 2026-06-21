package com.rideflow.driver.domain.model;

import com.rideflow.driver.domain.exception.IllegalDriverStateException;
import com.rideflow.driver.support.DriverFixtures;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pure unit tests for the {@link Driver} aggregate transitions. */
class DriverTest {

    private static final UUID ID = UUID.randomUUID();
    private static final Instant TS = Instant.parse("2026-06-20T10:00:00Z");

    // ------------------------------------------------------------------
    // register
    // ------------------------------------------------------------------

    @Test
    void register_startsOffline_andNormalizesIdentity() {
        Driver d = Driver.register("  Driver@RideFlow.Test ", " +9112345 ", "  Jane Doe ",
                "hashed:pw", VehicleType.XL, " ka01ab1234 ");
        assertThat(d.availability()).isEqualTo(DriverAvailability.OFFLINE);
        assertThat(d.email()).isEqualTo("driver@rideflow.test");
        assertThat(d.phone()).isEqualTo("+9112345");
        assertThat(d.fullName()).isEqualTo("Jane Doe");
        assertThat(d.vehiclePlate()).isEqualTo("KA01AB1234");
        assertThat(d.lastLocation()).isNull();
        assertThat(d.id()).isNotNull();
    }

    // ------------------------------------------------------------------
    // goOnline
    // ------------------------------------------------------------------

    @Test
    void goOnline_fromOffline() {
        Driver d = DriverFixtures.offline(ID).goOnline();
        assertThat(d.availability()).isEqualTo(DriverAvailability.ONLINE);
    }

    @Test
    void goOnline_idempotentWhenAlreadyOnline() {
        Driver online = DriverFixtures.online(ID);
        Driver d = online.goOnline();
        assertThat(d).isSameAs(online); // no-op returns same instance
    }

    @Test
    void goOnline_forbiddenWhileOnTrip() {
        Driver onTrip = DriverFixtures.onTrip(ID);
        assertThatThrownBy(onTrip::goOnline)
                .isInstanceOf(IllegalDriverStateException.class)
                .hasMessageContaining("ONLINE while ON_TRIP");
    }

    // ------------------------------------------------------------------
    // goOffline
    // ------------------------------------------------------------------

    @Test
    void goOffline_fromOnline() {
        Driver d = DriverFixtures.online(ID).goOffline();
        assertThat(d.availability()).isEqualTo(DriverAvailability.OFFLINE);
    }

    @Test
    void goOffline_idempotentWhenAlreadyOffline() {
        Driver offline = DriverFixtures.offline(ID);
        assertThat(offline.goOffline()).isSameAs(offline);
    }

    @Test
    void goOffline_forbiddenWhileOnTrip() {
        Driver onTrip = DriverFixtures.onTrip(ID);
        assertThatThrownBy(onTrip::goOffline)
                .isInstanceOf(IllegalDriverStateException.class)
                .hasMessageContaining("OFFLINE while ON_TRIP");
    }

    // ------------------------------------------------------------------
    // goOnTrip
    // ------------------------------------------------------------------

    @Test
    void goOnTrip_fromOnline() {
        Driver d = DriverFixtures.online(ID).goOnTrip();
        assertThat(d.availability()).isEqualTo(DriverAvailability.ON_TRIP);
    }

    @Test
    void goOnTrip_idempotentWhenAlreadyOnTrip() {
        Driver onTrip = DriverFixtures.onTrip(ID);
        assertThat(onTrip.goOnTrip()).isSameAs(onTrip);
    }

    @Test
    void goOnTrip_forbiddenWhenOffline() {
        Driver offline = DriverFixtures.offline(ID);
        assertThatThrownBy(offline::goOnTrip)
                .isInstanceOf(IllegalDriverStateException.class)
                .hasMessageContaining("cannot start a trip unless ONLINE");
    }

    // ------------------------------------------------------------------
    // endTrip — tolerant
    // ------------------------------------------------------------------

    @Test
    void endTrip_fromOnTrip_returnsOnline() {
        Driver d = DriverFixtures.onTrip(ID).endTrip();
        assertThat(d.availability()).isEqualTo(DriverAvailability.ONLINE);
    }

    @Test
    void endTrip_tolerantWhenNotOnTrip_online() {
        Driver online = DriverFixtures.online(ID);
        assertThat(online.endTrip()).isSameAs(online);
    }

    @Test
    void endTrip_tolerantWhenNotOnTrip_offline() {
        Driver offline = DriverFixtures.offline(ID);
        assertThat(offline.endTrip()).isSameAs(offline);
    }

    // ------------------------------------------------------------------
    // updateLocation
    // ------------------------------------------------------------------

    @Test
    void updateLocation_whileOnline() {
        GeoPoint point = new GeoPoint(13.1, 77.7);
        Driver d = DriverFixtures.online(ID).updateLocation(point, TS);
        assertThat(d.lastLocation()).isEqualTo(point);
        assertThat(d.lastLocationAt()).isEqualTo(TS);
        assertThat(d.availability()).isEqualTo(DriverAvailability.ONLINE);
    }

    @Test
    void updateLocation_whileOnTrip() {
        GeoPoint point = new GeoPoint(13.1, 77.7);
        Driver d = DriverFixtures.onTrip(ID).updateLocation(point, TS);
        assertThat(d.lastLocation()).isEqualTo(point);
        assertThat(d.availability()).isEqualTo(DriverAvailability.ON_TRIP);
    }

    @Test
    void updateLocation_forbiddenWhileOffline() {
        Driver offline = DriverFixtures.offline(ID);
        GeoPoint point = new GeoPoint(13.1, 77.7);
        assertThatThrownBy(() -> offline.updateLocation(point, TS))
                .isInstanceOf(IllegalDriverStateException.class)
                .hasMessageContaining("cannot update location while OFFLINE");
    }

    @Test
    void updateLocation_nullPointThrows() {
        Driver online = DriverFixtures.online(ID);
        assertThatThrownBy(() -> online.updateLocation(null, TS))
                .isInstanceOf(NullPointerException.class);
    }

    // ------------------------------------------------------------------
    // immutability
    // ------------------------------------------------------------------

    @Test
    void transitionReturnsNewInstance_originalUnchanged() {
        Driver offline = DriverFixtures.offline(ID);
        Driver online = offline.goOnline();
        assertThat(online).isNotSameAs(offline);
        assertThat(offline.availability()).isEqualTo(DriverAvailability.OFFLINE);
        assertThat(online.availability()).isEqualTo(DriverAvailability.ONLINE);
        // identity preserved
        assertThat(online.id()).isEqualTo(offline.id());
        assertThat(online.email()).isEqualTo(offline.email());
    }
}
