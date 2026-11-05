package com.florachain.order;

import com.florachain.inventory.StockDeductionStrategy;

import java.time.Duration;
import java.time.LocalDateTime;

/** 订单超时关闭并释放库存（FR-O06）。生产环境由延迟队列或定时任务触发。 */
public class OrderTimeoutService {
    private final OrderRepository repository;
    private final StockDeductionStrategy stock;

    public OrderTimeoutService(OrderRepository repository, StockDeductionStrategy stock) {
        this.repository = repository;
        this.stock = stock;
    }

    public int closeTimeoutOrders(LocalDateTime now, Duration ttl) {
        int closed = 0;
        for (Order o : repository.findByStatusCreatedBefore(OrderStatus.PENDING_PAYMENT, now.minus(ttl))) {
            if (o.transit(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED, "timeout-job")) {
                stock.release(o.skuId(), o.quantity(), o.orderNo());
                closed++;
            }
        }
        return closed;
    }
}
