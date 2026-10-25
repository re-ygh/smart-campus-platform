package com.smartcampus.marketplace.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private static final String TICKET_EVENTS_QUEUE = "ticket.events";

    public void publish(String eventType, Map<String, Object> payload) {
        try {
            Map<String, Object> message = Map.of(
                    "eventType", eventType,
                    "payload", payload,
                    "timestamp", System.currentTimeMillis()
            );
            rabbitTemplate.convertAndSend(TICKET_EVENTS_QUEUE, message);
            log.info("Published ticket event: {} with payload: {}", eventType, payload);
        } catch (Exception e) {
            log.error("Failed to publish ticket event: {}", eventType, e);
        }
    }
}

