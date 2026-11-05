package com.florachain.delivery;

import java.math.BigDecimal;

/** 按重量：超出 freeKg 的部分每公斤加收 perKg。 */
public class WeightFeeRule extends DeliveryFeeRule {
    private final double freeKg;
    private final BigDecimal perKg;

    public WeightFeeRule(BigDecimal baseFee, BigDecimal freeThreshold, double freeKg, BigDecimal perKg) {
        super(baseFee, freeThreshold);
        this.freeKg = freeKg;
        this.perKg = perKg;
    }

    @Override
    protected BigDecimal extra(DeliveryContext ctx) {
        long over = (long) Math.ceil(Math.max(0, ctx.weightKg() - freeKg));
        return perKg.multiply(BigDecimal.valueOf(over));
    }
}
