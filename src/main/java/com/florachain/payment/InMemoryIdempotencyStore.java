package com.florachain.payment;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIdempotencyStore implements IdempotencyStore {
    private final Set<String> keys = ConcurrentHashMap.newKeySet();

    @Override
    public boolean markIfAbsent(String key) {
        return keys.add(key);
    }
}
