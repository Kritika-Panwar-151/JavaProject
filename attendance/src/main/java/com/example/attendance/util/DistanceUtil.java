package com.example.attendance.util;

public class DistanceUtil {

    private static final double EARTH_RADIUS_METERS = 6371000;

    /**
     * Calculates distance between two GPS coordinates using Haversine formula.
     *
     * @return distance in meters
     */
    public static double distanceMeters(
            double lat1, double lon1,
            double lat2, double lon2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Convenience method for classroom validation
     */
    public static boolean isWithinRange(
            double teacherLat,
            double teacherLon,
            double studentLat,
            double studentLon,
            double maxMeters) {

        return distanceMeters(
                teacherLat, teacherLon,
                studentLat, studentLon
        ) <= maxMeters;
    }
}
