package com.smartcampus.notification.web;

import com.smartcampus.notification.model.Notification;
import com.smartcampus.notification.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Notification> create(@RequestBody @Valid NotificationRequest request) {
        return ResponseEntity.ok(notificationService.create(request.user(), request.message()));
    }

    @GetMapping
    public ResponseEntity<List<Notification>> list() {
        return ResponseEntity.ok(notificationService.forCurrentTenant());
    }

    @GetMapping("/my")
    public ResponseEntity<List<Notification>> myNotifications(org.springframework.security.core.Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(notificationService.forUser(username));
    }

    @RabbitListener(queues = "order.events")
    public void handleOrderEvents(Map<String, Object> payload) {
        System.out.println(">>> Received order event: " + payload);
        String userId = (String) payload.getOrDefault("userId", "system");
        String tenantId = (String) payload.getOrDefault("tenantId", "faculty-a");
        String status = (String) payload.getOrDefault("status", "UNKNOWN");
        Object orderIdObj = payload.get("orderId");
        if (orderIdObj != null) {
            Long orderId = orderIdObj instanceof Integer ? ((Integer) orderIdObj).longValue() : (Long) orderIdObj;
            notificationService.createWithTenant(userId, tenantId, "Order #" + orderId + " " + status.toLowerCase() + "!");
            System.out.println(">>> Created notification for user: " + userId + " in tenant: " + tenantId);
        }
    }

    @RabbitListener(queues = "reservation.events")
    public void handleReservationEvents(Map<String, Object> payload) {
        System.out.println(">>> Received reservation event: " + payload);
        String userId = (String) payload.getOrDefault("userId", "system");
        String tenantId = (String) payload.getOrDefault("tenantId", "faculty-a");
        String resourceName = (String) payload.getOrDefault("resourceName", "a resource");
        Object reservationIdObj = payload.get("reservationId");
        if (reservationIdObj != null) {
            Long reservationId = reservationIdObj instanceof Integer ? ((Integer) reservationIdObj).longValue() : (Long) reservationIdObj;
            notificationService.createWithTenant(userId, tenantId, "Reservation #" + reservationId + " for " + resourceName + " created!");
            System.out.println(">>> Created notification for user: " + userId + " in tenant: " + tenantId);
        }
    }

    public record NotificationRequest(@NotBlank String user, @NotBlank String message) {}
}

