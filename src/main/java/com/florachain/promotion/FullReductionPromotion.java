package com.florachain.promotion;

import java.math.BigDecimal;

/** 满减：满 threshold 减 reduction。 */
public class FullReductionPromotion implements PromotionStrategy {
    private final BigDecimal threshold;
    private final BigDecimal reduction;

    public FullReductionPromotion(BigDecimal threshold, BigDecimal reduction) {
        this.threshold = threshold;
        this.reduction = reduction;
    }

    @Override
    public String name() { return "满减"; }

    @Override
    public boolean applicable(PromotionContext ctx) { return ctx.amount().compareTo(threshold) >= 0; }

    @Override
    public BigDecimal discount(PromotionContext ctx) { return reduction; }
}
