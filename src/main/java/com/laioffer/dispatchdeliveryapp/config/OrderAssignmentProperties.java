package com.laioffer.dispatchdeliveryapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "order-assignment")
public record OrderAssignmentProperties(int minBatteryLevel, double deliverySpeed) {}
