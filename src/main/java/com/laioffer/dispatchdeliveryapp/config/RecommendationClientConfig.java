package com.laioffer.dispatchdeliveryapp.config;

import com.laioffer.dispatchdeliveryapp.service.recommendation.KeywordRecommendationClient;
import com.laioffer.dispatchdeliveryapp.service.recommendation.OpenAiRecommendationClient;
import com.laioffer.dispatchdeliveryapp.service.recommendation.RecommendationClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RecommendationClientConfig {

    @Bean
    RecommendationClient recommendationClient(AiProperties aiProperties, JsonMapper jsonMapper) {
        if (aiProperties.isConfigured()) {
            return new OpenAiRecommendationClient(aiProperties, jsonMapper);
        }
        return new KeywordRecommendationClient();
    }
}
