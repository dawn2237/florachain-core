package com.florachain.order;

import com.florachain.common.Result;
import com.florachain.infra.EventPublisher;
import com.florachain.infra.OrderCreatedEvent;
import com.florachain.infra.RateLimiter;
import com.florachain.inventory.DeductResult;
import com.florachain.inventory.StockDeductionStrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限量抢购下单（FR-O08、FR-O01）。
 * 处理顺序：限流 → 缓存预扣库存 → 创建订单 → 发布事件由消息队列异步落库。
 */
public class FlashSaleService {
    private final RateLimiter limiter;
    private final StockDeductionStrategy stock;
    private final OrderRepository repository;
    private final EventPublisher publisher;
    private final AtomicLong seq = new AtomicLong();

    public FlashSaleService(RateLimiter limiter, StockDeductionStrategy stock,
                            OrderRepository repository, EventPublisher publisher) {
        this.limiter = limiter;
        this.stock = stock;
        this.repository = repository;
        this.publisher = publisher;
    }

    public Result<String> buy(long userId, String skuId, int quantity, BigDecimal price, String deliverySlot) {
        if (!limiter.tryAcquire()) {
            return Result.fail(429, "系统繁忙，请稍后重试");
        }
        String orderNo = "FC" + seq.incrementAndGet();
        DeductResult r = stock.deduct(skuId, quantity, orderNo);
        if (r == DeductResult.INSUFFICIENT) {
            return Result.fail(410, "商品已抢完");
        }
        Order order = new Order(orderNo, userId, skuId, quantity,
                price.multiply(BigDecimal.valueOf(quantity)), deliverySlot, LocalDateTime.now());
        repository.save(order);
        publisher.publish(new OrderCreatedEvent(orderNo, userId));
        return Result.ok(orderNo);
    }
}
