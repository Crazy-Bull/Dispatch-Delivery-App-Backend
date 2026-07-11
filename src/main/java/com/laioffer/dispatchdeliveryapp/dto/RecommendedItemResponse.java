package com.laioffer.dispatchdeliveryapp.dto;

public record RecommendedItemResponse(
        Long productId,
        Integer quantity,
        String reason,
        ProductResponse product
) {}
