package com.laioffer.dispatchdeliveryapp.service.recommendation;

import com.laioffer.dispatchdeliveryapp.dto.ProductResponse;

import java.util.List;

public interface RecommendationClient {

    RawRecommendation recommend(String query, List<ProductResponse> catalog);
}
