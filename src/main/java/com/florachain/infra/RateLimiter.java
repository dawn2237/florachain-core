package com.florachain.infra;

public interface RateLimiter {
    boolean tryAcquire();
}
