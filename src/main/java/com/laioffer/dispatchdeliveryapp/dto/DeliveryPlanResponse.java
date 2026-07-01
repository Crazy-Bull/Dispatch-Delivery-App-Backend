package com.laioffer.dispatchdeliveryapp.dto;

import java.math.BigDecimal;

public record DeliveryPlanResponse(
        Long stationId,
        String stationName,
        String stationAddress,
        BigDecimal totalAmount,
        Double distanceKm,
        Integer availableDrones,
        boolean feasible,
        String infeasibilityReason
) {}
