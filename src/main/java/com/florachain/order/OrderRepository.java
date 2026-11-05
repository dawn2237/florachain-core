package com.florachain.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 订单仓储接口，生产环境由 MySQL 实现，这里提供内存实现用于验证。 */
public interface OrderRepository {
    void save(Order order);

    Optional<Order> find(String orderNo);

    List<Order> findByStatusCreatedBefore(OrderStatus status, LocalDateTime time);

    int count();
}
