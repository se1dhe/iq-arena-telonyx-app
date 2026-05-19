package com.se1dhe.iqarena.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Настройки Redis rate limiting для чувствительных HTTP endpoint.
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int maxRequests,
        long windowSeconds,
        String keyPrefix,
        List<String> protectedPaths
) {
}
