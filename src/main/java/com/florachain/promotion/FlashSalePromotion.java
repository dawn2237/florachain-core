package com.florachain.promotion;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 限时折扣：只对抢购商品生效，rate 为折后比例，如 0.8 表示八折。 */
public class FlashSalePromotion implements PromotionStrategy {
    private final BigDecimal rate;

    public FlashSalePromotion(BigDecimal rate) {
        this.rate = rate;
    }

    @Override
    public String name() { return "限时折扣"; }

    @Override
    public boolean applicable(PromotionContext ctx) { return ctx.flashSaleItem(); }

    @Override
    public BigDecimal discount(PromotionContext ctx) {
        return ctx.amount().multiply(BigDecimal.ONE.subtract(rate)).setScale(2, RoundingMode.HALF_UP);
    }
}
