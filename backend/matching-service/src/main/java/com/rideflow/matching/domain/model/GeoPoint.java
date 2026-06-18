package com.rideflow.matching.domain.model;

/**
 * Immutable lat/lng value object. {@code lat} = latitude, {@code lng} = longitude.
 * Redis-Geo uses (lng, lat) — adapters perform the swap; domain is order-safe.
 */
public record GeoPoint(double lat, double lng) {

    public GeoPoint {
        if (lat < -90.0  || lat > 90.0)   throw new IllegalArgumentException("lat out of range: " + lat);
        if (lng < -180.0 || lng > 180.0)  throw new IllegalArgumentException("lng out of range: " + lng);
    }

    /**
     * Initial bearing in degrees [0, 360) from {@code this} to {@code other},
     * measured clockwise from true north. Used by the scorer to compute
     * heading-alignment.
     */
    public double bearingTo(GeoPoint other) {
        double lat1 = Math.toRadians(this.lat);
        double lat2 = Math.toRadians(other.lat);
        double dLng = Math.toRadians(other.lng - this.lng);

        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                 - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360.0) % 360.0;
    }
}
