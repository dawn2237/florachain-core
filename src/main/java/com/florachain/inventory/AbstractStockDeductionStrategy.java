package com.florachain.inventory;

import com.florachain.common.BusinessException;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 模板方法：公共的参数校验与幂等控制放在这里，子类只实现具体的扣减与释放。 */
public abstract class AbstractStockDeductionStrategy implements StockDeductionStrategy {
    private final Set<String> reserved = ConcurrentHashMap.newKeySet();

    @Override
    public final DeductResult deduct(String skuId, int quantity, String orderNo) {
        if (quantity <= 0) {
            throw new BusinessException(4000, "购买数量必须大于0");
        }
        String key = orderNo + "#" + skuId;
        if (!reserved.add(key)) {
            return DeductResult.DUPLICATE;
        }
        try {
            if (doReserve(skuId, quantity)) {
                return DeductResult.SUCCESS;
            }
            reserved.remove(key);
            return DeductResult.INSUFFICIENT;
        } catch (RuntimeException e) {
            reserved.remove(key);
            throw e;
        }
    }

    @Override
    public final boolean release(String skuId, int quantity, String orderNo) {
        if (!reserved.remove(orderNo + "#" + skuId)) {
            return false;
        }
        doRelease(skuId, quantity);
        return true;
    }

    protected abstract boolean doReserve(String skuId, int quantity);

    protected abstract void doRelease(String skuId, int quantity);
}
