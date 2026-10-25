package com.smartcampus.notification.repository;

import com.smartcampus.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTenantIdAndUser(String tenantId, String user);
    List<Notification> findByUserAndTenantId(String user, String tenantId);
    List<Notification> findByTenantId(String tenantId);
}

