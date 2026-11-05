package com.florachain.inventory;

import com.florachain.common.BusinessException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 对照方案：数据库乐观锁（UPDATE ... WHERE version = ?）。
 * 真实数据库中每次更新都要经过网络往返，抢购时同一行被集中更新，版本冲突与重试会显著增加，
 * 这是 CR-002 放弃该方案、改用缓存预扣的原因；本类在内存中模拟，只用于对照，不能代替压测结论。
 */
public class OptimisticLockStrategy extends AbstractStockDeductionStrategy {
    private record Row(int stock, long version) {
    }

    private final ConcurrentHashMap<String, AtomicReference<Row>> table = new ConcurrentHashMap<>();
    private final int maxRetry;

    public OptimisticLockStrategy(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public void initStock(String skuId, int quantity) {
        table.put(skuId, new AtomicReference<>(new Row(quantity, 0)));
    }

    @Override
    protected boolean doReserve(String skuId, int quantity) {
        AtomicReference<Row> ref = table.get(skuId);
        for (int i = 0; i < maxRetry; i++) {
            Row cur = ref.get();
            if (cur.stock() < quantity) {
                return false;
            }
            if (ref.compareAndSet(cur, new Row(cur.stock() - quantity, cur.version() + 1))) {
                return true;
            }
        }
        throw new BusinessException(4090, "库存更新冲突，请重试");
    }

    @Override
    protected void doRelease(String skuId, int quantity) {
        AtomicReference<Row> ref = table.get(skuId);
        ref.updateAndGet(r -> new Row(r.stock() + quantity, r.version() + 1));
    }

    @Override
    public int remaining(String skuId) {
        return table.get(skuId).get().stock();
    }
}
