package com.florachain.inventory;

/** 缓存抽象：生产环境由 Redis 的 Lua 脚本实现，保证"判断并扣减"是一个原子操作。 */
public interface StockCache {
    void set(String key, int value);

    /** 库存足够时扣减并返回扣减后的数量，不足时返回 -1。 */
    long decrementIfEnough(String key, int quantity);

    void increment(String key, int quantity);

    int get(String key);
}
