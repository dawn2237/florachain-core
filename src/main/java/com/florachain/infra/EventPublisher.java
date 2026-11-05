package com.florachain.infra;

/** 事件发布接口，生产环境由 RabbitMQ 实现。 */
public interface EventPublisher {
    void publish(DomainEvent event);
}
