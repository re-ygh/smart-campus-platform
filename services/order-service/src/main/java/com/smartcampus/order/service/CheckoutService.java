package com.smartcampus.order.service;

import com.smartcampus.order.messaging.OrderEventPublisher;
import com.smartcampus.order.model.Order;
import com.smartcampus.order.model.OrderItem;
import com.smartcampus.order.model.OrderStatus;
import com.smartcampus.order.repository.OrderRepository;
import com.smartcampus.order.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Value("${app.marketplace.url:http://localhost:8083}")
    private String marketplaceBase;

    @Transactional
    public Order checkout(String username, List<ItemRequest> items, String authHeader) {
        Order order = Order.builder()
                .userId(username)
                .tenantId(TenantContext.getTenantId())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
        orderRepository.save(order);

        List<ItemRequest> reserved = new ArrayList<>();
        List<Map<String, Object>> ticketDetails = new ArrayList<>();
        try {
            for (ItemRequest item : items) {
                reserveInventory(item, authHeader);
                reserved.add(item);
                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .ticketId(item.ticketId())
                        .quantity(item.quantity())
                        .build();
                order.getItems().add(orderItem);
                
                // Get ticket details for notification
                try {
                    Map<String, Object> ticket = getTicketDetails(item.ticketId(), authHeader);
                    ticketDetails.add(ticket);
                } catch (Exception ex) {
                    log.warn("Failed to get ticket details for notification: {}", ex.getMessage());
                }
            }
            // mock payment step
            if (Math.random() < 0.1) {
                throw new IllegalStateException("Mock payment failure");
            }
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
            
            // Build detailed event payload
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("orderId", order.getId());
            eventPayload.put("userId", username);
            eventPayload.put("status", "CONFIRMED");
            eventPayload.put("tenantId", order.getTenantId());
            eventPayload.put("tickets", ticketDetails);
            
            eventPublisher.publish("order.confirmed", eventPayload);
        } catch (Exception e) {
            compensate(reserved, authHeader);
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
            eventPublisher.publish("order.failed", Map.of("orderId", order.getId(), "userId", username, "status", "FAILED", "reason", e.getMessage(), "tenantId", order.getTenantId()));
        }
        return order;
    }

    private void reserveInventory(ItemRequest item, String authHeader) {
        Map<String, Object> body = new HashMap<>();
        body.put("ticketId", item.ticketId());
        body.put("quantity", item.quantity());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                marketplaceBase + "/tickets/reserve",
                HttpMethod.POST,
                request,
                (Class<Map<String, Object>>)(Class<?>)Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException("Reserve failed");
        }
    }

    private Map<String, Object> getTicketDetails(Long ticketId, String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
                marketplaceBase + "/tickets/" + ticketId,
                HttpMethod.GET,
                request,
                Map.class);
        
        return response.getBody();
    }

    private void compensate(List<ItemRequest> reserved, String authHeader) {
        for (ItemRequest item : reserved) {
            try {
                Map<String, Object> body = Map.of("ticketId", item.ticketId(), "quantity", item.quantity());
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set(HttpHeaders.AUTHORIZATION, authHeader);
                restTemplate.postForEntity(marketplaceBase + "/tickets/release", new HttpEntity<>(body, headers), Void.class);
            } catch (Exception ignored) {
            }
        }
    }

    public record ItemRequest(Long ticketId, int quantity) {}
}

