package com.smartcampus.exam.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    private final RestTemplate restTemplate;
    
    @Value("${app.notification.url:http://localhost:8086}")
    private String baseUrl;

    @CircuitBreaker(name = "notification", fallbackMethod = "fallback")
    public void sendExamStarted(String tenantId, String user, Long examId, String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }
        Map<String, Object> body = Map.of(
                "user", user,
                "tenantId", tenantId,
                "message", "Exam " + examId + " started");
        restTemplate.postForEntity(baseUrl + "/notifications", new HttpEntity<>(body, headers), Void.class);
    }

    private void fallback(String tenantId, String user, Long examId, String authHeader, Throwable t) {
        log.warn("Notification service unavailable, stored locally. examId={}, user={}", examId, user);
    }
}

