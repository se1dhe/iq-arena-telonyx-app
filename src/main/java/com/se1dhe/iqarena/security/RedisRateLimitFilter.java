package com.se1dhe.iqarena.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

// Ограничивает частоту запросов к auth/realtime endpoint через Redis INCR + TTL.
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(RateLimitProperties.class)
public class RedisRateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.enabled() || !isProtectedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String key = rateLimitKey(request);
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(properties.windowSeconds()));
            }

            long current = count == null ? 1 : count;
            response.setHeader("X-RateLimit-Limit", String.valueOf(properties.maxRequests()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, properties.maxRequests() - current)));

            if (current > properties.maxRequests()) {
                writeLimitExceeded(response);
                return;
            }

            filterChain.doFilter(request, response);
        } catch (RuntimeException ex) {
            log.warn("Redis rate limit check failed", ex);
            writeRateLimitUnavailable(response);
        }
    }

    private boolean isProtectedPath(String path) {
        return properties.protectedPaths().stream().anyMatch(path::startsWith);
    }

    private String rateLimitKey(HttpServletRequest request) {
        String pathGroup = request.getRequestURI()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ":");
        return properties.keyPrefix() + ":" + pathGroup + ":" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeLimitExceeded(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(properties.windowSeconds()));
        response.setContentType("application/json");
        response.getWriter().write("""
                {"code":"RATE_LIMIT_EXCEEDED","message":"Слишком много запросов","retryAfterSeconds":%d}
                """.formatted(properties.windowSeconds()));
    }

    private void writeRateLimitUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(503);
        response.setContentType("application/json");
        response.getWriter().write("""
                {"code":"RATE_LIMIT_UNAVAILABLE","message":"Rate limiting временно недоступен"}
                """);
    }
}
