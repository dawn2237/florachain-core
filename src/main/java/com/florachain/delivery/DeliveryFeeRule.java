package com.florachain.delivery;

import java.math.BigDecimal;

/** 运费规则（FR-D01）：模板方法固定"起步价加附加费、满额包邮"，附加费由子类决定。 */
public abstract class DeliveryFeeRule {
    private final BigDecimal baseFee;
    private final BigDecimal freeThreshold;

    protected DeliveryFeeRule(BigDecimal baseFee, BigDecimal freeThreshold) {
        this.baseFee = baseFee;
        this.freeThreshold = freeThreshold;
    }

    public final BigDecimal fee(DeliveryContext ctx) {
        if (ctx.orderAmount().compareTo(freeThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return baseFee.add(extra(ctx));
    }

    protected abstract BigDecimal extra(DeliveryContext ctx);
}
