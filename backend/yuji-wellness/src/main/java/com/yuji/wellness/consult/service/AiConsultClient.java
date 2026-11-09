package com.yuji.wellness.consult.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

/**
 * 可选的 AI 陪伴文案服务。AI 只负责"把话说得更有温度"，
 * 医学相关内容始终由本地审核过的知识卡提供；AI 不可用时整条链路不受影响。
 */
@Component
public class AiConsultClient {

    private final RestClient client;

    public AiConsultClient(@Value("${yuji.ai.base-url:http://localhost:8000}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(300);
        factory.setReadTimeout(900);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @SuppressWarnings("unchecked")
    public Optional<String> reply(String message, String topicTitle) {
        try {
            Map<String, Object> body = client.post()
                    .uri("/api/v1/consult/reply")
                    .body(Map.of("message", message, "topic", topicTitle == null ? "" : topicTitle))
                    .retrieve()
                    .body(Map.class);
            if (body == null) {
                return Optional.empty();
            }
            Object reply = body.get("reply");
            return reply == null || reply.toString().isBlank()
                    ? Optional.empty() : Optional.of(reply.toString());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
