package com.smartcampus.gateway.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Aggregates health status of all backend microservices
 * Accessible via /actuator/health
 */
@Component
public class ServicesHealthIndicator implements ReactiveHealthIndicator {

    private final WebClient webClient;
    
    private final Map<String, String> services = Map.of(
        "auth-service", "http://localhost:8081",
        "booking-service", "http://localhost:8082",
        "marketplace-service", "http://localhost:8083",
        "order-service", "http://localhost:8084",
        "exam-service", "http://localhost:8085",
        "notification-service", "http://localhost:8086",
        "iot-service", "http://localhost:8087"
    );

    public ServicesHealthIndicator() {
        this.webClient = WebClient.builder()
                .build();
    }

    @Override
    public Mono<Health> health() {
        Map<String, String> details = new HashMap<>();
        
        // Check each service health
        return Mono.fromCallable(() -> {
            services.forEach((name, url) -> {
                try {
                    webClient.get()
                            .uri(url + "/actuator/health")
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(2))
                            .block();
                    details.put(name, "UP");
                } catch (Exception e) {
                    details.put(name, "DOWN");
                }
            });
            
            // Check if any service is down
            boolean anyDown = details.values().stream().anyMatch(status -> status.equals("DOWN"));
            
            return anyDown 
                ? Health.down().withDetails(details).build()
                : Health.up().withDetails(details).build();
        });
    }
}

