package com.laioffer.dispatchdeliveryapp.model;

public enum DeliveryMode {
    DRONE("无人机"),
    ROBOT("地面机器人");

    private final String displayName;

    DeliveryMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static DeliveryMode fromStringOrDefault(String value, DeliveryMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return DeliveryMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
