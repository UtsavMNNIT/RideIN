package com.rideflow.trip.domain.model;

/** Immutable lat/lng value object. */
public record GeoPoint(double lat, double lng) {

    public GeoPoint {
        if (lat < -90.0  || lat > 90.0)  throw new IllegalArgumentException("lat out of range: " + lat);
        if (lng < -180.0 || lng > 180.0) throw new IllegalArgumentException("lng out of range: " + lng);
    }
}
