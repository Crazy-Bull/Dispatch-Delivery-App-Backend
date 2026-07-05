package com.laioffer.dispatchdeliveryapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "order-assignment")
public record OrderAssignmentProperties(
        int minBatteryLevel,
        double deliverySpeed,
        double robotDeliverySpeed) {
    // Canonical 3-arg ctor. Spring binds it via @ConstructorBinding so
    // robot-delivery-speed can fall back to a sane default if not set.
    @ConstructorBinding
    public OrderAssignmentProperties {
    }

    // 2-arg ctor kept for backward-compatibility in tests/legacy yaml.
    public OrderAssignmentProperties(int minBatteryLevel, double deliverySpeed) {
        this(minBatteryLevel, deliverySpeed, 6.0);
    }
}
