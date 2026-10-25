package com.smartcampus.booking.service;

import com.smartcampus.booking.messaging.ReservationEventPublisher;
import com.smartcampus.booking.model.Reservation;
import com.smartcampus.booking.model.Resource;
import com.smartcampus.booking.repository.ReservationRepository;
import com.smartcampus.booking.repository.ResourceRepository;
import com.smartcampus.booking.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationEventPublisher eventPublisher;

    public Resource createResource(Resource resource) {
        resource.setTenantId(TenantContext.getTenantId());
        return resourceRepository.save(resource);
    }

    public List<Resource> listResources() {
        return resourceRepository.findByTenantId(TenantContext.getTenantId());
    }

    public List<Reservation> listReservations() {
        return reservationRepository.findByTenantId(TenantContext.getTenantId());
    }

    @Transactional
    public Reservation reserve(Long resourceId, String userId, LocalDateTime start, LocalDateTime end) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));
        if (!resource.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant access denied");
        }
        List<Reservation> conflicts = reservationRepository.findConflicts(resource, TenantContext.getTenantId(), start, end);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Resource already reserved in that time range");
        }
        Reservation reservation = Reservation.builder()
                .resource(resource)
                .startTime(start)
                .endTime(end)
                .userId(userId)
                .tenantId(TenantContext.getTenantId())
                .build();
        Reservation saved = reservationRepository.save(reservation);
        eventPublisher.publish("reservation.created", Map.of(
                "reservationId", saved.getId(),
                "userId", userId,
                "resourceName", resource.getName(),
                "tenantId", saved.getTenantId()
        ));
        return saved;
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));
        if (!resource.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant access denied");
        }
        // Delete all reservations for this resource first
        List<Reservation> reservations = reservationRepository.findByResourceAndTenantId(resource, TenantContext.getTenantId());
        reservationRepository.deleteAll(reservations);
        resourceRepository.delete(resource);
    }

    @Transactional
    public void cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        if (!reservation.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant access denied");
        }
        String userId = reservation.getUserId();
        String resourceName = reservation.getResource().getName();
        reservationRepository.delete(reservation);
        eventPublisher.publish("reservation.cancelled", Map.of(
                "reservationId", reservationId,
                "userId", userId,
                "resourceName", resourceName,
                "tenantId", reservation.getTenantId()
        ));
    }

    public List<Reservation> listUserReservations(String userId) {
        return reservationRepository.findByUserIdAndTenantId(userId, TenantContext.getTenantId());
    }
}

