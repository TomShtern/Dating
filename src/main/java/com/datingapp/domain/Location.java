package com.datingapp.domain;

/**
 * Value object representing geographic coordinates.
 */
public record Location(double lat, double lon) {
    private static final double EARTH_RADIUS_KM = 6371.0;

    public Location {
        if (lat < -90 || lat > 90)
            throw new IllegalArgumentException("Invalid latitude: " + lat);
        if (lon < -180 || lon > 180)
            throw new IllegalArgumentException("Invalid longitude: " + lon);
    }

    public Distance distanceTo(Location other) {
        double km = haversine(this.lat, this.lon, other.lat, other.lon);
        return Distance.ofKilometers(km);
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
