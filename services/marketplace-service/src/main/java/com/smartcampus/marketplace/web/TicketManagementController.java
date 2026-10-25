package com.smartcampus.marketplace.web;

import com.smartcampus.marketplace.model.Ticket;
import com.smartcampus.marketplace.service.TicketService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets/manage")
@RequiredArgsConstructor
public class TicketManagementController {

    private final TicketService ticketService;

    @PatchMapping("/{id}/assign-teacher")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<Ticket> assignTeacher(@PathVariable Long id, @RequestBody AssignRequest request) {
        return ResponseEntity.ok(ticketService.assignTeacher(id, request.teacherUsername()));
    }

    public record AssignRequest(@NotBlank String teacherUsername) {}
}

