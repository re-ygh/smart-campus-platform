package com.smartcampus.order.web;

import com.smartcampus.order.model.Order;
import com.smartcampus.order.repository.OrderRepository;
import com.smartcampus.order.service.CheckoutService;
import com.smartcampus.order.service.CheckoutService.ItemRequest;
import com.smartcampus.order.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutService checkoutService;
    private final OrderRepository orderRepository;

    @PostMapping("/checkout")
    public ResponseEntity<Order> checkout(@RequestBody @Valid CheckoutRequest request,
                                          Authentication authentication,
                                          @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        List<ItemRequest> items = request.items().stream()
                .map(i -> new ItemRequest(i.ticketId(), i.quantity()))
                .collect(Collectors.toList());
        Order order = checkoutService.checkout(authentication.getName(), items, authHeader);
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<List<Order>> list() {
        return ResponseEntity.ok(orderRepository.findByTenantId(TenantContext.getTenantId()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Order>> myOrders(Authentication authentication) {
        String userId = authentication.getName();
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(orderRepository.findByUserIdAndTenantId(userId, tenantId));
    }

    @GetMapping("/by-ticket/{ticketId}")
    public ResponseEntity<List<Order>> ordersByTicket(@PathVariable Long ticketId) {
        String tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(orderRepository.findByTicketIdAndTenantId(ticketId, tenantId));
    }

    public record CheckoutItem(@NotNull Long ticketId, @Min(1) int quantity) {}
    public record CheckoutRequest(List<CheckoutItem> items) {}
}

