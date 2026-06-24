package com.laioffer.dispatchdeliveryapp.util;

public final class GeographyUtils {

    private GeographyUtils() {}

    public static String toGeographyWkt(String pointWkt) {
        String normalized = pointWkt.strip();
        if (normalized.startsWith("SRID=")) {
            return normalized;
        }
        return "SRID=4326;" + normalized;
    }

    public static String toPointWkt(String pointWkt) {
        String normalized = pointWkt.strip();
        if (normalized.startsWith("SRID=")) {
            int separator = normalized.indexOf(';');
            return separator >= 0 ? normalized.substring(separator + 1) : normalized;
        }
        return normalized;
    }
}
