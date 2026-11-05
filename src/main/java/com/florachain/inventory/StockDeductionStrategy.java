package com.florachain.inventory;

/** 库存扣减策略接口（FR-S04）：以订单号保证幂等。 */
public interface StockDeductionStrategy {
    DeductResult deduct(String skuId, int quantity, String orderNo);

    /** 释放之前预扣的库存；订单号不存在或已释放时返回 false。 */
    boolean release(String skuId, int quantity, String orderNo);

    int remaining(String skuId);
}
