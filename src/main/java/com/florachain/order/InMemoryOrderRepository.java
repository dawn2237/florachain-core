package com.florachain.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryOrderRepository implements OrderRepository {
    private final ConcurrentMap<String, Order> store = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) {
        store.put(order.orderNo(), order);
    }

    @Override
    public Optional<Order> find(String orderNo) {
        return Optional.ofNullable(store.get(orderNo));
    }

    @Override
    public List<Order> findByStatusCreatedBefore(OrderStatus status, LocalDateTime time) {
        return store.values().stream()
                .filter(o -> o.status() == status && !o.createdAt().isAfter(time))
                .toList();
    }

    @Override
    public int count() {
        return store.size();
    }
}
