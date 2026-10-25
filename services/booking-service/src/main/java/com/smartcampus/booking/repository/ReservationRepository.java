package com.smartcampus.booking.repository;

import com.smartcampus.booking.model.Reservation;
import com.smartcampus.booking.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.resource = :resource and r.tenantId = :tenantId and " +
            "((r.startTime <= :endTime and r.endTime >= :startTime))")
    List<Reservation> findConflicts(@Param("resource") Resource resource,
                                    @Param("tenantId") String tenantId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    List<Reservation> findByTenantId(String tenantId);
    
    List<Reservation> findByResourceAndTenantId(Resource resource, String tenantId);
    
    List<Reservation> findByUserIdAndTenantId(String userId, String tenantId);
}

