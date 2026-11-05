package com.florachain.payment;

/** 幂等存储：生产环境用 Redis 的 SETNX 加过期时间实现。 */
public interface IdempotencyStore {
    /** 第一次登记返回 true，重复登记返回 false。 */
    boolean markIfAbsent(String key);
}
