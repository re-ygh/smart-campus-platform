package com.smartcampus.booking.security;

import com.smartcampus.booking.service.JwtService;
import com.smartcampus.booking.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        log.debug("JWT Filter - Path: {}", request.getRequestURI());
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.debug("JWT Filter - Auth Header: {}", (authHeader != null ? "Present" : "Missing"));
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("JWT Filter - No valid Bearer token, continuing...");
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            final String jwt = authHeader.substring(7);
            String username = jwtService.extractUsername(jwt);
            log.debug("JWT Filter - Extracted username: {}", username);
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                String role = jwtService.extractRole(jwt);
                log.debug("JWT Filter - Extracted role: {}", role);
                
                UserDetails userDetails = User.builder()
                        .username(username)
                        .password("")
                        .authorities("ROLE_" + role)
                        .build();
                        
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    TenantContext.setTenantId(jwtService.extractTenant(jwt));
                    log.debug("JWT Filter - Authentication successful for: {}", username);
                } else {
                    log.warn("JWT Filter - Token validation failed!");
                }
            }
        } catch (Exception e) {
            log.error("JWT Filter - Error: {}", e.getMessage(), e);
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

