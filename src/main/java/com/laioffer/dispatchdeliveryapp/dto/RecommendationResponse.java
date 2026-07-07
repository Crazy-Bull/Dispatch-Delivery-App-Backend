package com.laioffer.dispatchdeliveryapp.dto;

import java.util.List;

public record RecommendationResponse(
        String summary,
        List<RecommendedItemResponse> items
) {}
