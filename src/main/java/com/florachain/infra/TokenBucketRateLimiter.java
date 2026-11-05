package com.florachain.infra;

/** 令牌桶限流：capacity 为突发上限，ratePerSecond 为持续速率。 */
public class TokenBucketRateLimiter implements RateLimiter {
    private final double capacity;
    private final double ratePerSecond;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucketRateLimiter(double capacity, double ratePerSecond) {
        this.capacity = capacity;
        this.ratePerSecond = ratePerSecond;
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    @Override
    public synchronized boolean tryAcquire() {
        long now = System.nanoTime();
        tokens = Math.min(capacity, tokens + (now - lastRefillNanos) / 1_000_000_000.0 * ratePerSecond);
        lastRefillNanos = now;
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }
}
