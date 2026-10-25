package com.smartcampus.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    
    @Bean
    public Queue orderEventsQueue() {
        return new Queue("order.events", true);
    }
    
    @Bean
    public Queue reservationEventsQueue() {
        return new Queue("reservation.events", true);
    }
    
    @Bean
    public Queue examEventsQueue() {
        return new Queue("exam.events", true);
    }
    
    @Bean
    public Queue ticketEventsQueue() {
        return new Queue("ticket.events", true);
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

