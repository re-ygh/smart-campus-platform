package com.smartcampus.marketplace.service;

import com.smartcampus.marketplace.messaging.TicketEventPublisher;
import com.smartcampus.marketplace.model.Ticket;
import com.smartcampus.marketplace.model.TicketType;
import com.smartcampus.marketplace.repository.TicketRepository;
import com.smartcampus.marketplace.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketEventPublisher eventPublisher;

    public Ticket create(String title, String desc, TicketType type, BigDecimal price, int inventory, String assignedTeacher, String createdBy) {
        Ticket ticket = Ticket.builder()
                .title(title)
                .description(desc)
                .type(type)
                .price(price)
                .inventory(inventory)
                .assignedTeacher(assignedTeacher)
                .tenantId(TenantContext.getTenantId())
                .createdBy(createdBy)
                .build();
        Ticket saved = ticketRepository.save(ticket);
        eventPublisher.publish("ticket.created", Map.of(
                "id", saved.getId(),
                "title", saved.getTitle(),
                "type", saved.getType().toString(),
                "assignedTeacher", saved.getAssignedTeacher(),
                "createdBy", createdBy,
                "tenantId", saved.getTenantId()
        ));
        return saved;
    }

    public List<Ticket> list() {
        return ticketRepository.findByTenantId(TenantContext.getTenantId());
    }

    public List<Ticket> listByTeacher(String teacherUsername) {
        return ticketRepository.findByAssignedTeacherAndTenantId(teacherUsername, TenantContext.getTenantId());
    }

    public Ticket assignTeacher(Long id, String teacherUsername) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        enforceTenant(ticket);
        ticket.setAssignedTeacher(teacherUsername);
        return ticketRepository.save(ticket);
    }

    @Transactional
    public boolean reserveInventory(Long id, int qty) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        enforceTenant(ticket);
        if (ticket.getInventory() < qty) {
            return false;
        }
        ticket.setInventory(ticket.getInventory() - qty);
        ticketRepository.save(ticket);
        return true;
    }

    @Transactional
    public void releaseInventory(Long id, int qty) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        enforceTenant(ticket);
        ticket.setInventory(ticket.getInventory() + qty);
        ticketRepository.save(ticket);
    }

    @Transactional
    public void deleteTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        enforceTenant(ticket);
        ticketRepository.delete(ticket);
    }

    private void enforceTenant(Ticket ticket) {
        if (!ticket.getTenantId().equals(TenantContext.getTenantId())) {
            throw new IllegalArgumentException("Cross-tenant access denied");
        }
    }
}

