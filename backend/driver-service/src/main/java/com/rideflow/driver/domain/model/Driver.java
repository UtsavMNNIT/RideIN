package com.rideflow.driver.domain.model;

import com.rideflow.driver.domain.exception.IllegalDriverStateException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The driver aggregate root.
 *
 * <p>Immutable by design: every state transition returns a <em>new</em>
 * {@code Driver} rather than mutating in place, which keeps business rules
 * total (an invalid {@code Driver} can never exist) and makes the use-case
 * layer trivially testable. The persistence layer maps this snapshot onto a
 * mutable JPA entity.
 *
 * <p>Business invariants enforced here, not in the controller or service:
 * <ul>
 *   <li>A freshly registered driver starts {@link DriverAvailability#OFFLINE}.</li>
 *   <li>Location may only be updated while ONLINE or ON_TRIP.</li>
 *   <li>A driver cannot go OFFLINE while ON_TRIP (a ride is in progress).</li>
 * </ul>
 *
 * <p>{@code passwordHash} is part of the aggregate because the driver-service
 * owns driver credentials. It is never exposed through an API DTO.
 */
public record Driver(
        UUID               id,
        String             email,
        String             phone,
        String             fullName,
        String             passwordHash,
        VehicleType        vehicleType,
        String             vehiclePlate,
        DriverAvailability availability,
        GeoPoint           lastLocation,    // nullable until the first location ping
        Instant            lastLocationAt,  // nullable, paired with lastLocation
        Instant            createdAt,
        Instant            updatedAt
) {

    public Driver {
        Objects.requireNonNull(id,           "id");
        Objects.requireNonNull(email,        "email");
        Objects.requireNonNull(phone,        "phone");
        Objects.requireNonNull(fullName,     "fullName");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(vehicleType,  "vehicleType");
        Objects.requireNonNull(vehiclePlate, "vehiclePlate");
        Objects.requireNonNull(availability, "availability");
        Objects.requireNonNull(createdAt,    "createdAt");
        Objects.requireNonNull(updatedAt,    "updatedAt");
    }

    /**
     * Factory for a brand-new driver. Normalizes identity fields (email lower-cased,
     * plate upper-cased) so uniqueness checks and lookups are case-insensitive.
     * The caller supplies an already-hashed password — the domain never sees the
     * raw secret.
     */
    public static Driver register(String email, String phone, String fullName,
                                  String passwordHash, VehicleType vehicleType,
                                  String vehiclePlate) {
        Instant now = Instant.now();
        return new Driver(
                UUID.randomUUID(),
                normalizeEmail(email),
                phone.trim(),
                fullName.trim(),
                passwordHash,
                vehicleType,
                vehiclePlate.trim().toUpperCase(),
                DriverAvailability.OFFLINE,
                null,
                null,
                now,
                now
        );
    }

    /** Transition to ONLINE. Idempotent if already ONLINE. */
    public Driver goOnline() {
        if (availability == DriverAvailability.ONLINE) {
            return this;
        }
        if (availability == DriverAvailability.ON_TRIP) {
            throw new IllegalDriverStateException(id, "cannot go ONLINE while ON_TRIP");
        }
        return withAvailability(DriverAvailability.ONLINE);
    }

    /** Transition to OFFLINE. Idempotent if already OFFLINE; forbidden mid-trip. */
    public Driver goOffline() {
        if (availability == DriverAvailability.OFFLINE) {
            return this;
        }
        if (availability == DriverAvailability.ON_TRIP) {
            throw new IllegalDriverStateException(id, "cannot go OFFLINE while ON_TRIP");
        }
        return withAvailability(DriverAvailability.OFFLINE);
    }

    /**
     * Record a new location reading. Only meaningful while the driver is
     * accepting work — an OFFLINE driver pushing locations is a client bug.
     */
    public Driver updateLocation(GeoPoint point, Instant capturedAt) {
        Objects.requireNonNull(point,      "point");
        Objects.requireNonNull(capturedAt, "capturedAt");
        if (availability == DriverAvailability.OFFLINE) {
            throw new IllegalDriverStateException(id, "cannot update location while OFFLINE");
        }
        return new Driver(
                id, email, phone, fullName, passwordHash, vehicleType, vehiclePlate,
                availability, point, capturedAt, createdAt, Instant.now()
        );
    }

    private Driver withAvailability(DriverAvailability newAvailability) {
        return new Driver(
                id, email, phone, fullName, passwordHash, vehicleType, vehiclePlate,
                newAvailability, lastLocation, lastLocationAt, createdAt, Instant.now()
        );
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
