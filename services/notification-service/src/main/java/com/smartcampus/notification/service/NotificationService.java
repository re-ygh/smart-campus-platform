package com.smartcampus.notification.service;

import com.smartcampus.notification.model.Notification;
import com.smartcampus.notification.repository.NotificationRepository;
import com.smartcampus.notification.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification create(String user, String message) {
        String tenant = TenantContext.getTenantId();
        if (tenant == null) {
            tenant = "system";
        }
        Notification n = Notification.builder()
                .user(user)
                .tenantId(tenant)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(n);
    }

    public Notification createWithTenant(String user, String tenantId, String message) {
        Notification n = Notification.builder()
                .user(user)
                .tenantId(tenantId)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(n);
    }

    public List<Notification> forCurrentTenant() {
        return notificationRepository.findByTenantId(TenantContext.getTenantId());
    }

    public List<Notification> forUser(String username) {
        return notificationRepository.findByUserAndTenantId(username, TenantContext.getTenantId());
    }
}

