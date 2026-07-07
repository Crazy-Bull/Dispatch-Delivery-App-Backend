package com.laioffer.dispatchdeliveryapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        String apiKey,
        String model,
        String baseUrl
) {
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
