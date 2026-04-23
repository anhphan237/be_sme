package com.sme.be_sme.modules.ai.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api-key:}")
    private String apiKey;

    @Bean
    public OpenAIClient openAIClient() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("openai.api-key is missing");
        }

        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}