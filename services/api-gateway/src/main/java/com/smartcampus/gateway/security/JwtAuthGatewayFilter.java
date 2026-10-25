package com.smartcampus.gateway.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthGatewayFilter implements WebFilter {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        log.debug("Incoming request: {} {}", exchange.getRequest().getMethod(), path);
        
        if (path.startsWith("/auth/login") || path.contains("actuator/health")) {
            log.debug("Bypassing auth for: {}", path);
            return chain.filter(exchange);
        }
        
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.debug("Auth Header: {}", (authHeader != null ? "Present" : "Missing"));
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("No valid Bearer token, returning 401");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        try {
            String token = authHeader.substring(7);
            Claims claims = jwtService.parse(token);
            String username = claims.getSubject();
            String role = (String) claims.get("role");
            String tenantId = (String) claims.get("tenantId");
            
            log.debug("JWT parsed successfully - User: {}, Role: {}, Tenant: {}", username, role, tenantId);
            
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User", username)
                    .header("X-Role", role)
                    .header("X-Tenant-Id", tenantId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .build();
                    
            log.debug("Forwarding request with Authorization header");
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            log.error("JWT parsing failed: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}

