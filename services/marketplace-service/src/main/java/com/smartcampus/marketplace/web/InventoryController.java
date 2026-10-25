package com.smartcampus.marketplace.web;

import com.smartcampus.marketplace.service.TicketService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final TicketService ticketService;

    @PostMapping("/reserve")
    public ResponseEntity<?> reserve(@RequestBody ReserveRequest request) {
        boolean ok = ticketService.reserveInventory(request.ticketId(), request.quantity());
        if (!ok) {
            return ResponseEntity.status(409).body(Map.of("error", "Not enough inventory"));
        }
        return ResponseEntity.ok(Map.of("status", "reserved"));
    }

    @PostMapping("/release")
    public ResponseEntity<?> release(@RequestBody ReserveRequest request) {
        ticketService.releaseInventory(request.ticketId(), request.quantity());
        return ResponseEntity.ok(Map.of("status", "released"));
    }

    public record ReserveRequest(@NotNull Long ticketId, @Min(1) int quantity) {}
}

