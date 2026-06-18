package com.rideflow.driver.domain.model;

/**
 * Immutable WGS-84 coordinate. Validates range on construction so an invalid
 * point can never exist in the domain.
 */
public record GeoPoint(double lat, double lng) {

    public GeoPoint {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("lat out of range [-90,90]: " + lat);
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new IllegalArgumentException("lng out of range [-180,180]: " + lng);
        }
    }
}
