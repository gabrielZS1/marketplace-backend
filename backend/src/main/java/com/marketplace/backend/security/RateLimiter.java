package com.marketplace.backend.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter em memória (token bucket), sem dependência externa.
 *
 * Suficiente para uma instância única. Se o backend rodar em várias instâncias,
 * migrar para Bucket4j + Redis para o limite ser compartilhado.
 */
@Component
public class RateLimiter {

    private static final long IDLE_EVICT_MS = 30 * 60 * 1000L;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @param key             identificador (ex: "login:1.2.3.4")
     * @param capacity         nº máximo de tokens (rajada)
     * @param refillPerWindow  tokens repostos a cada janela
     * @param windowSeconds    duração da janela em segundos
     * @return true se a requisição pode passar
     */
    public boolean allow(String key, int capacity, int refillPerWindow, int windowSeconds) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity));
        return bucket.tryConsume(capacity, refillPerWindow, windowSeconds * 1000L);
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000L)
    void evictIdle() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().lastAccessMs > IDLE_EVICT_MS);
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefillMs;
        private volatile long lastAccessMs;

        Bucket(int capacity) {
            this.tokens = capacity;
            this.lastRefillMs = System.currentTimeMillis();
            this.lastAccessMs = this.lastRefillMs;
        }

        synchronized boolean tryConsume(int capacity, int refillPerWindow, long windowMs) {
            long now = System.currentTimeMillis();
            this.lastAccessMs = now;

            double elapsed = now - lastRefillMs;
            if (elapsed > 0) {
                double refill = (elapsed / windowMs) * refillPerWindow;
                tokens = Math.min(capacity, tokens + refill);
                lastRefillMs = now;
            }

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
