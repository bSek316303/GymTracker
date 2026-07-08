package org.key0.gymtracker.enums;

public enum TrackingParameter {
    REPETITIONS, TIME, DISTANCE, CALORIES;

    public static TrackingParameter fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return TrackingParameter.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
