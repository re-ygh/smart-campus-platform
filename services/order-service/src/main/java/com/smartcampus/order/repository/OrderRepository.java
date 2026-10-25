package com.smartcampus.order.repository;

import com.smartcampus.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByTenantId(String tenantId);
    List<Order> findByUserIdAndTenantId(String userId, String tenantId);
    
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.ticketId = :ticketId AND o.tenantId = :tenantId AND o.status = 'CONFIRMED'")
    List<Order> findByTicketIdAndTenantId(@Param("ticketId") Long ticketId, @Param("tenantId") String tenantId);
}

