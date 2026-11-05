package com.florachain.inventory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** 用 ConcurrentHashMap 的原子 compute 模拟 Redis 单线程执行 Lua 脚本的效果。 */
public class InMemoryStockCache implements StockCache {
    private final ConcurrentHashMap<String, Integer> data = new ConcurrentHashMap<>();

    @Override
    public void set(String key, int value) {
        data.put(key, value);
    }

    @Override
    public long decrementIfEnough(String key, int quantity) {
        AtomicLong result = new AtomicLong(-1);
        data.computeIfPresent(key, (k, v) -> {
            if (v >= quantity) {
                result.set(v - quantity);
                return v - quantity;
            }
            return v;
        });
        return result.get();
    }

    @Override
    public void increment(String key, int quantity) {
        data.merge(key, quantity, Integer::sum);
    }

    @Override
    public int get(String key) {
        return data.getOrDefault(key, 0);
    }
}
