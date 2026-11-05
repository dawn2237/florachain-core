package com.florachain.inventory;

/** 方案：缓存预扣库存（CR-002 采用）。扣减发生在缓存，数据库落库由消息队列异步完成。 */
public class RedisPreReserveStrategy extends AbstractStockDeductionStrategy {
    private final StockCache cache;

    public RedisPreReserveStrategy(StockCache cache) {
        this.cache = cache;
    }

    public void initStock(String skuId, int quantity) {
        cache.set(key(skuId), quantity);
    }

    private static String key(String skuId) {
        return "stock:" + skuId;
    }

    @Override
    protected boolean doReserve(String skuId, int quantity) {
        return cache.decrementIfEnough(key(skuId), quantity) >= 0;
    }

    @Override
    protected void doRelease(String skuId, int quantity) {
        cache.increment(key(skuId), quantity);
    }

    @Override
    public int remaining(String skuId) {
        return cache.get(key(skuId));
    }
}
