package com.rideflow.rider.domain.model;

/**
 * Immutable WGS-84 coordinate, range-validated on construction.
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
