package com.smartcampus.exam.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExamEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    private static final String EXAM_EVENTS_QUEUE = "exam.events";

    public void publish(String eventType, Map<String, Object> payload) {
        try {
            Map<String, Object> message = Map.of(
                    "eventType", eventType,
                    "payload", payload,
                    "timestamp", System.currentTimeMillis()
            );
            rabbitTemplate.convertAndSend(EXAM_EVENTS_QUEUE, message);
            log.info("Published exam event: {} with payload: {}", eventType, payload);
        } catch (Exception e) {
            log.error("Failed to publish exam event: {}", eventType, e);
        }
    }
}

