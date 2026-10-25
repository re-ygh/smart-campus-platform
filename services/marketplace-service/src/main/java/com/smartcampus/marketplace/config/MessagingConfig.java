package com.smartcampus.marketplace.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    @Bean
    public Queue ticketEventsQueue() {
        return new Queue("ticket.events", true);
    }
}

