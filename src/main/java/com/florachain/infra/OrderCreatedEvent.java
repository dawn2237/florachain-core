package com.florachain.infra;

/** 订单已创建事件：消费者负责把订单写入数据库，实现削峰。 */
public record OrderCreatedEvent(String orderNo, long userId) implements DomainEvent {
    @Override
    public String eventId() {
        return "order-created:" + orderNo;
    }
}
