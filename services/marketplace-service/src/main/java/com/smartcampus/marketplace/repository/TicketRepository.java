package com.smartcampus.marketplace.repository;

import com.smartcampus.marketplace.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByTenantId(String tenantId);
    List<Ticket> findByAssignedTeacherAndTenantId(String assignedTeacher, String tenantId);
}

