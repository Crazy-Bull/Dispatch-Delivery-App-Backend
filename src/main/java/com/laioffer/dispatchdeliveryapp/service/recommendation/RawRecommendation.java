package com.laioffer.dispatchdeliveryapp.service.recommendation;

import java.util.List;

public record RawRecommendation(
        String summary,
        List<RawRecommendedItem> items
) {}
