package com.smartcampus.marketplace.web;

import com.smartcampus.marketplace.model.Ticket;
import com.smartcampus.marketplace.model.TicketType;
import com.smartcampus.marketplace.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<Ticket> create(@RequestBody @Valid CreateTicketRequest request, Authentication authentication) {
        Ticket saved = ticketService.create(request.title(), request.description(), request.type(),
                request.price(), request.inventory(), request.assignedTeacher(), authentication.getName());
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> list() {
        return ResponseEntity.ok(ticketService.list());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<Ticket>> myTickets(Authentication authentication) {
        return ResponseEntity.ok(ticketService.listByTeacher(authentication.getName()));
    }

    @DeleteMapping("/{ticketId}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> delete(@PathVariable Long ticketId) {
        ticketService.deleteTicket(ticketId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(@RequestBody @Valid ReserveRequest request) {
        boolean success = ticketService.reserveInventory(request.ticketId(), request.quantity());
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(409).body("Insufficient inventory");
        }
    }

    @PostMapping("/release")
    public ResponseEntity<?> release(@RequestBody @Valid ReserveRequest request) {
        ticketService.releaseInventory(request.ticketId(), request.quantity());
        return ResponseEntity.ok().build();
    }

    public record CreateTicketRequest(@NotBlank String title,
                                      String description,
                                      @NotNull TicketType type,
                                      @NotNull BigDecimal price,
                                      @Min(0) int inventory,
                                      String assignedTeacher) {}

    public record ReserveRequest(@NotNull Long ticketId, @Min(1) int quantity) {}
}

