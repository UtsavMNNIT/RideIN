package com.rideflow.pricing.domain.model;

/**
 * Immutable lat/lng value object. {@code lat} = latitude, {@code lng} = longitude.
 */
public record GeoPoint(double lat, double lng) {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    public GeoPoint {
        if (lat < -90.0  || lat > 90.0)   throw new IllegalArgumentException("lat out of range: " + lat);
        if (lng < -180.0 || lng > 180.0)  throw new IllegalArgumentException("lng out of range: " + lng);
    }

    /**
     * Great-circle (haversine) distance in kilometres from {@code this} to
     * {@code other}. This is the straight-line distance; the {@code TripEstimator}
     * scales it by a road-factor to approximate real driving distance.
     */
    public double haversineKmTo(GeoPoint other) {
        double dLat = Math.toRadians(other.lat - this.lat);
        double dLng = Math.toRadians(other.lng - this.lng);
        double lat1 = Math.toRadians(this.lat);
        double lat2 = Math.toRadians(other.lat);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.sin(dLng / 2) * Math.sin(dLng / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
