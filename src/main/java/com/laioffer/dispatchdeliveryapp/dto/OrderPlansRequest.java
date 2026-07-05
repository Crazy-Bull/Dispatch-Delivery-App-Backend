package com.laioffer.dispatchdeliveryapp.dto;

import java.util.List;

public record OrderPlansRequest(
        Double longitude,
        Double latitude,
        List<OrderItemRequest> items
) {}
