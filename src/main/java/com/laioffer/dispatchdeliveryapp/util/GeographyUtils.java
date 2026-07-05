package com.laioffer.dispatchdeliveryapp.util;

import com.laioffer.dispatchdeliveryapp.dto.GeoPoint;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GeographyUtils {

    private static final Pattern POINT_PATTERN =
            Pattern.compile("POINT\\s*\\(\\s*([-\\d.]+)\\s+([-\\d.]+)\\s*\\)", Pattern.CASE_INSENSITIVE);

    private GeographyUtils() {}

    public static Optional<GeoPoint> parseGeoPoint(String pointWkt) {
        if (pointWkt == null || pointWkt.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = POINT_PATTERN.matcher(pointWkt);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new GeoPoint(
                Double.parseDouble(matcher.group(1)),
                Double.parseDouble(matcher.group(2))));
    }

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
