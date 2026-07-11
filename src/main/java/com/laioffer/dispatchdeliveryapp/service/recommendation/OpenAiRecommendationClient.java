package com.laioffer.dispatchdeliveryapp.service.recommendation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.laioffer.dispatchdeliveryapp.config.AiProperties;
import com.laioffer.dispatchdeliveryapp.dto.ProductResponse;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

public class OpenAiRecommendationClient implements RecommendationClient {

    private static final String SYSTEM_PROMPT = """
            You are a grocery shopping assistant for a drone delivery service in San Francisco.
            Given the user's natural-language request and the available product catalog, recommend items ONLY from that catalog.
            Respond with valid JSON (no markdown fences) in this exact shape:
            {"summary":"<one sentence overview>","items":[{"product_id":<number>,"quantity":<positive integer>,"reason":"<short reason>"}]}
            Rules:
            - Use only product_id values present in the catalog.
            - quantity must be at least 1 and must not exceed the product's stock.
            - Recommend 1-6 items unless the user asks for more.
            - If nothing fits, return an empty items array and explain in summary.
            """;

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String model;

    public OpenAiRecommendationClient(AiProperties aiProperties, JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        this.model = aiProperties.model() != null && !aiProperties.model().isBlank()
                ? aiProperties.model()
                : "gpt-4o-mini";
        String baseUrl = aiProperties.baseUrl() != null && !aiProperties.baseUrl().isBlank()
                ? aiProperties.baseUrl()
                : "https://api.openai.com/v1";
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + aiProperties.apiKey())
                .build();
    }

    @Override
    public RawRecommendation recommend(String query, List<ProductResponse> catalog) {
        String catalogJson;
        try {
            catalogJson = jsonMapper.writeValueAsString(catalog);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize product catalog", e);
        }

        String userMessage = "User request: " + query + "\n\nProduct catalog:\n" + catalogJson;

        Map<String, Object> body = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userMessage)
                )
        );

        ChatCompletionResponse response = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(ChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("Empty response from AI provider");
        }

        String content = response.choices().getFirst().message().content();
        try {
            return jsonMapper.readValue(content, RawRecommendation.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse AI recommendation response", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletionResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Message(String content) {}
}
