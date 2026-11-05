package com.florachain.delivery;

import java.math.BigDecimal;

/** 按距离：超出 freeKm 的部分每公里加收 perKm（不足1公里按1公里计）。 */
public class DistanceFeeRule extends DeliveryFeeRule {
    private final double freeKm;
    private final BigDecimal perKm;

    public DistanceFeeRule(BigDecimal baseFee, BigDecimal freeThreshold, double freeKm, BigDecimal perKm) {
        super(baseFee, freeThreshold);
        this.freeKm = freeKm;
        this.perKm = perKm;
    }

    @Override
    protected BigDecimal extra(DeliveryContext ctx) {
        long over = (long) Math.ceil(Math.max(0, ctx.distanceKm() - freeKm));
        return perKm.multiply(BigDecimal.valueOf(over));
    }
}
