package com.smartcampus.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    /**
     * Rate limiting based on user's IP address
     * For authenticated users, you could use username from JWT
     */
    @Bean
    public KeyResolver ipAddressKeyResolver() {
        return exchange -> {
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            String address = remoteAddress != null && remoteAddress.getAddress() != null
                    ? remoteAddress.getAddress().getHostAddress() 
                    : "unknown";
            return Mono.just(address);
        };
    }

    /**
     * Alternative: Rate limiting based on username from JWT
     * Uncomment this if you want to rate limit per user instead of per IP
     */
    /*
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String username = exchange.getRequest().getHeaders().getFirst("X-User-Name");
            return Mono.just(username != null ? username : "anonymous");
        };
    }
    */
}

