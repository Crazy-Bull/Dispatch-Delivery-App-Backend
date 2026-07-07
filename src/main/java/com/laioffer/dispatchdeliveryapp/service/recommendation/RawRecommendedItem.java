package com.laioffer.dispatchdeliveryapp.service.recommendation;

public record RawRecommendedItem(
        Long productId,
        Integer quantity,
        String reason
) {}
