package com.laioffer.dispatchdeliveryapp.service.recommendation;

import com.laioffer.dispatchdeliveryapp.dto.ProductResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KeywordRecommendationClient implements RecommendationClient {

    private static final Map<String, Set<String>> KEYWORD_HINTS = Map.ofEntries(
            Map.entry("brunch", Set.of("avocado", "sourdough", "coffee", "salad", "matcha")),
            Map.entry("breakfast", Set.of("sourdough", "coffee", "matcha", "avocado")),
            Map.entry("morning", Set.of("coffee", "matcha", "sourdough")),
            Map.entry("healthy", Set.of("salad", "avocado", "salmon", "sparkling")),
            Map.entry("light", Set.of("salad", "sparkling", "avocado")),
            Map.entry("dinner", Set.of("salmon", "burrito", "salad", "sourdough")),
            Map.entry("meal", Set.of("salmon", "burrito", "salad")),
            Map.entry("vegan", Set.of("burrito", "avocado", "salad", "matcha")),
            Map.entry("vegetarian", Set.of("burrito", "avocado", "salad", "sourdough")),
            Map.entry("snack", Set.of("chocolate", "avocado")),
            Map.entry("dessert", Set.of("chocolate", "matcha")),
            Map.entry("sweet", Set.of("chocolate", "matcha")),
            Map.entry("drink", Set.of("coffee", "sparkling", "matcha")),
            Map.entry("drinks", Set.of("coffee", "sparkling", "matcha")),
            Map.entry("hydrate", Set.of("sparkling", "coffee")),
            Map.entry("party", Set.of("sparkling", "avocado", "chocolate", "sourdough")),
            Map.entry("friends", Set.of("sparkling", "avocado", "sourdough", "salad")),
            Map.entry("protein", Set.of("salmon", "burrito")),
            Map.entry("fish", Set.of("salmon")),
            Map.entry("seafood", Set.of("salmon")),
            Map.entry("coffee", Set.of("coffee", "matcha")),
            Map.entry("caffeine", Set.of("coffee", "matcha")),
            Map.entry("salad", Set.of("salad")),
            Map.entry("bread", Set.of("sourdough")),
            Map.entry("chocolate", Set.of("chocolate"))
    );

    @Override
    public RawRecommendation recommend(String query, List<ProductResponse> catalog) {
        String normalized = query.toLowerCase(Locale.ROOT);
        Map<Long, ScoredProduct> scores = new LinkedHashMap<>();

        for (ProductResponse product : catalog) {
            if (product.stock() == null || product.stock() <= 0) {
                continue;
            }
            int score = scoreProduct(normalized, product);
            if (score > 0) {
                scores.put(product.id(), new ScoredProduct(product, score));
            }
        }

        if (scores.isEmpty()) {
            List<ProductResponse> inStock = catalog.stream()
                    .filter(p -> p.stock() != null && p.stock() > 0)
                    .limit(3)
                    .toList();
            List<RawRecommendedItem> fallbackItems = inStock.stream()
                    .map(p -> new RawRecommendedItem(p.id(), 1, "Popular pick from our catalog"))
                    .toList();
            return new RawRecommendation(
                    "Here are a few popular items you might like.",
                    fallbackItems);
        }

        List<ScoredProduct> ranked = scores.values().stream()
                .sorted(Comparator.comparingInt(ScoredProduct::score).reversed())
                .limit(5)
                .toList();

        List<RawRecommendedItem> items = ranked.stream()
                .map(sp -> new RawRecommendedItem(
                        sp.product().id(),
                        1,
                        reasonFor(sp.product(), normalized)))
                .collect(Collectors.toCollection(ArrayList::new));

        String summary = buildSummary(query, items.size());
        return new RawRecommendation(summary, items);
    }

    private int scoreProduct(String query, ProductResponse product) {
        String haystack = (product.name() + " " + product.description()).toLowerCase(Locale.ROOT);
        int score = 0;

        for (String token : query.split("[^a-z0-9]+")) {
            if (token.length() < 3) {
                continue;
            }
            if (haystack.contains(token)) {
                score += 3;
            }
        }

        for (Map.Entry<String, Set<String>> entry : KEYWORD_HINTS.entrySet()) {
            if (query.contains(entry.getKey())) {
                for (String hint : entry.getValue()) {
                    if (haystack.contains(hint)) {
                        score += 2;
                    }
                }
            }
        }

        return score;
    }

    private String reasonFor(ProductResponse product, String query) {
        String name = product.name().toLowerCase(Locale.ROOT);
        if (query.contains("healthy") && (name.contains("salad") || name.contains("avocado"))) {
            return "Fits your healthy eating goal";
        }
        if (query.contains("brunch") && name.contains("sourdough")) {
            return "Classic brunch staple";
        }
        if (query.contains("vegan") && name.contains("burrito")) {
            return "Plant-based meal option";
        }
        if (query.contains("dinner") && name.contains("salmon")) {
            return "Great centerpiece for dinner";
        }
        return "Matches your request: \"" + truncate(query, 60) + "\"";
    }

    private String buildSummary(String query, int count) {
        return "Based on \"" + truncate(query, 80) + "\", we suggest " + count
                + " item" + (count == 1 ? "" : "s") + " from our catalog.";
    }

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 3) + "...";
    }

    private record ScoredProduct(ProductResponse product, int score) {}
}
