package com.smartcampus.booking.web;

import com.smartcampus.booking.model.Reservation;
import com.smartcampus.booking.service.BookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid ReserveRequest request, Authentication auth) {
        try {
            Reservation saved = bookingService.reserve(
                    request.resourceId(),
                    auth.getName(),
                    request.startTime(),
                    request.endTime()
            );
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Reservation>> list() {
        return ResponseEntity.ok(bookingService.listReservations());
    }

    @GetMapping("/my")
    public ResponseEntity<List<Reservation>> myReservations(Authentication auth) {
        return ResponseEntity.ok(bookingService.listUserReservations(auth.getName()));
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<?> cancel(@PathVariable Long reservationId) {
        try {
            bookingService.cancelReservation(reservationId);
            return ResponseEntity.ok(Map.of("message", "Reservation cancelled successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record ReserveRequest(@NotNull Long resourceId,
                                 @NotNull LocalDateTime startTime,
                                 @NotNull LocalDateTime endTime) {}
}

