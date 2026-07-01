package com.laioffer.dispatchdeliveryapp.dto;

import java.util.List;

public record CreateOrderRequest(
        Long stationId,
        Double longitude,
        Double latitude,
        List<OrderItemRequest> items
) {}
