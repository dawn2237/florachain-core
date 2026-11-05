package com.florachain.promotion;

import java.math.BigDecimal;

/** 优惠券：券码匹配时减免固定金额。 */
public class CouponPromotion implements PromotionStrategy {
    private final String code;
    private final BigDecimal value;

    public CouponPromotion(String code, BigDecimal value) {
        this.code = code;
        this.value = value;
    }

    @Override
    public String name() { return "优惠券"; }

    @Override
    public boolean applicable(PromotionContext ctx) { return code.equals(ctx.couponCode()); }

    @Override
    public BigDecimal discount(PromotionContext ctx) { return value; }
}
