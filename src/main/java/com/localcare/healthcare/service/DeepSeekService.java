package com.localcare.healthcare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DeepSeekService(
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.model:deepseek-chat}") String model,
            @Value("${deepseek.enabled:true}") boolean enabled,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(20_000);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isAvailable() {
        return enabled && StringUtils.hasText(apiKey);
    }

    public Optional<String> chat(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.2,
                    "stream", false
            );

            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (!StringUtils.hasText(responseBody)) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || !StringUtils.hasText(content.asText())) {
                return Optional.empty();
            }
            return Optional.of(content.asText());
        } catch (Exception e) {
            log.warn("DeepSeek API 调用失败，已回退本地规则：{}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<JsonNode> parseJsonObject(String content) {
        try {
            String json = extractJsonObject(content);
            if (!StringUtils.hasText(json)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readTree(json));
        } catch (Exception e) {
            log.warn("DeepSeek JSON 解析失败，已回退本地规则：{}", e.getMessage());
            return Optional.empty();
        }
    }

    private String extractJsonObject(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String value = content.trim();
        if (value.startsWith("```json")) {
            value = value.substring("```json".length()).trim();
        } else if (value.startsWith("```")) {
            value = value.substring("```".length()).trim();
        }
        if (value.endsWith("```")) {
            value = value.substring(0, value.length() - 3).trim();
        }

        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end < start) {
            return "";
        }
        return value.substring(start, end + 1);
    }
}
