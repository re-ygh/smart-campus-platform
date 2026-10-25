package com.smartcampus.booking.web;

import com.smartcampus.booking.model.Resource;
import com.smartcampus.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<Resource> create(@RequestBody @Valid Resource resource) {
        return ResponseEntity.ok(bookingService.createResource(resource));
    }

    @GetMapping
    public ResponseEntity<List<Resource>> list() {
        return ResponseEntity.ok(bookingService.listResources());
    }

    @DeleteMapping("/{resourceId}")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> delete(@PathVariable Long resourceId) {
        bookingService.deleteResource(resourceId);
        return ResponseEntity.ok().build();
    }
}

