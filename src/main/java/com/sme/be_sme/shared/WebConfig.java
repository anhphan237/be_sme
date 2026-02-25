package com.sme.be_sme.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CORS config so frontend (Vercel, localhost) can call the API.
 * Set app.cors.allowed-origins (comma-separated). Patterns like https://*.vercel.app supported.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,https://*.vercel.app}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> patterns = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (patterns.isEmpty()) {
            patterns = List.of("http://localhost:5173", "http://localhost:3000", "https://*.vercel.app");
        }

        // "*" = allow any origin (useful for dev/staging)
        String[] patternArr = patterns.toArray(new String[0]);
        registry.addMapping("/api/**")
                .allowedOriginPatterns(patternArr.length > 0 ? patternArr : new String[]{"*"})
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept")
                .exposedHeaders("Authorization")
                .maxAge(3600);
    }
}
