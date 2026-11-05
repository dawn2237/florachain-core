package com.florachain.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/** 订单聚合根。状态变更通过 CAS 完成，保证并发回调下只生效一次。 */
public class Order {
    public record StatusLog(OrderStatus from, OrderStatus to, String operator, LocalDateTime time) {
    }

    private final String orderNo;
    private final long userId;
    private final String skuId;
    private final int quantity;
    private final BigDecimal amount;
    private final String deliverySlot;           // 配送时间段（FR-O05，缺陷 BUG-005 要求的字段）
    private final LocalDateTime createdAt;
    private final AtomicReference<OrderStatus> status = new AtomicReference<>(OrderStatus.PENDING_PAYMENT);
    private final List<StatusLog> logs = new CopyOnWriteArrayList<>();

    public Order(String orderNo, long userId, String skuId, int quantity, BigDecimal amount,
                 String deliverySlot, LocalDateTime createdAt) {
        this.orderNo = orderNo;
        this.userId = userId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.amount = amount;
        this.deliverySlot = deliverySlot;
        this.createdAt = createdAt;
    }

    /** 期望当前状态为 expected 时才变更；返回 false 表示状态已被他人改变（幂等保护）。 */
    public boolean transit(OrderStatus expected, OrderStatus target, String operator) {
        OrderStateMachine.assertTransit(expected, target);
        if (status.compareAndSet(expected, target)) {
            logs.add(new StatusLog(expected, target, operator, LocalDateTime.now()));
            return true;
        }
        return false;
    }

    public String orderNo() { return orderNo; }
    public long userId() { return userId; }
    public String skuId() { return skuId; }
    public int quantity() { return quantity; }
    public BigDecimal amount() { return amount; }
    public String deliverySlot() { return deliverySlot; }
    public LocalDateTime createdAt() { return createdAt; }
    public OrderStatus status() { return status.get(); }
    public List<StatusLog> logs() { return logs; }
}
