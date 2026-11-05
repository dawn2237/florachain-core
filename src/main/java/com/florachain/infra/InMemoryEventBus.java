package com.florachain.infra;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** 内存事件总线：异步消费，模拟消息队列把写库压力从请求线程中拆出去。 */
public class InMemoryEventBus implements EventPublisher {
    private final List<Consumer<DomainEvent>> consumers = new CopyOnWriteArrayList<>();
    private final ExecutorService worker = Executors.newFixedThreadPool(4);

    public void subscribe(Consumer<DomainEvent> consumer) {
        consumers.add(consumer);
    }

    @Override
    public void publish(DomainEvent event) {
        worker.submit(() -> consumers.forEach(c -> c.accept(event)));
    }

    /** 等待已提交的事件处理完毕后关闭。 */
    public void drainAndClose() throws InterruptedException {
        worker.shutdown();
        worker.awaitTermination(30, TimeUnit.SECONDS);
    }
}
