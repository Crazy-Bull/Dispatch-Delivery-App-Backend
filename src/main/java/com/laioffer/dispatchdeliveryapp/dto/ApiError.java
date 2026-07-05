package com.laioffer.dispatchdeliveryapp.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiError(
        String message,
        Integer status,
        Instant timestamp,
        List<String> details
) {
    public static Map<String, Object> of(String message, int status) {
        return Map.of(
                "message", message == null ? "Unknown error" : message,
                "status", status,
                "timestamp", Instant.now().toString()
        );
    }
}
