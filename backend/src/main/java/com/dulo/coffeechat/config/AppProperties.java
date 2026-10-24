package com.dulo.coffeechat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String hostName, String timeZone, List<String> corsOrigins, Gemini gemini) {

    public record Gemini(String apiKey, String model, String baseUrl) {
    }
}
