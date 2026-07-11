package com.laioffer.dispatchdeliveryapp.service;

import com.laioffer.dispatchdeliveryapp.dto.ProductResponse;
import com.laioffer.dispatchdeliveryapp.dto.RecommendationResponse;
import com.laioffer.dispatchdeliveryapp.dto.RecommendedItemResponse;
import com.laioffer.dispatchdeliveryapp.service.recommendation.RawRecommendation;
import com.laioffer.dispatchdeliveryapp.service.recommendation.RawRecommendedItem;
import com.laioffer.dispatchdeliveryapp.service.recommendation.RecommendationClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final ProductService productService;
    private final RecommendationClient recommendationClient;

    public RecommendationService(ProductService productService, RecommendationClient recommendationClient) {
        this.productService = productService;
        this.recommendationClient = recommendationClient;
    }

    public RecommendationResponse recommend(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be blank");
        }

        List<ProductResponse> catalog = productService.getCatalog();
        RawRecommendation raw = recommendationClient.recommend(query.trim(), catalog);

        Map<Long, ProductResponse> catalogById = catalog.stream()
                .collect(Collectors.toMap(ProductResponse::id, Function.identity()));

        List<RecommendedItemResponse> items = new ArrayList<>();
        if (raw.items() != null) {
            for (RawRecommendedItem rawItem : raw.items()) {
                if (rawItem.productId() == null) {
                    continue;
                }
                ProductResponse product = catalogById.get(rawItem.productId());
                if (product == null || product.stock() == null || product.stock() <= 0) {
                    continue;
                }
                int quantity = rawItem.quantity() != null && rawItem.quantity() > 0
                        ? rawItem.quantity()
                        : 1;
                quantity = Math.min(quantity, product.stock());
                String reason = rawItem.reason() != null && !rawItem.reason().isBlank()
                        ? rawItem.reason()
                        : "Recommended for your request";
                items.add(new RecommendedItemResponse(product.id(), quantity, reason, product));
            }
        }

        String summary = raw.summary() != null && !raw.summary().isBlank()
                ? raw.summary()
                : items.isEmpty()
                        ? "We couldn't find matching items. Try browsing the catalog."
                        : "Here are items we think you'll like.";

        return new RecommendationResponse(summary, items);
    }
}
