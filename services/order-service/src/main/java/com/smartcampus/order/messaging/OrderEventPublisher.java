package com.smartcampus.order.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publish(String eventType, Map<String, Object> payload) {
        rabbitTemplate.convertAndSend("order.events", payload);
    }
}

