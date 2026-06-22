package com.laioffer.dispatchdeliveryapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mock-delivery")
public record MockDeliveryProperties(
        boolean enabled,
        long tickIntervalMs
) {
    public MockDeliveryProperties {
        if (tickIntervalMs <= 0) {
            tickIntervalMs = 5000;
        }
    }
}
