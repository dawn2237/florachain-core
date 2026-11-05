package com.florachain.promotion;

import java.math.BigDecimal;

/** 营销策略接口（FR-U08）：新增活动只需新增一个实现类，不改下单主流程。 */
public interface PromotionStrategy {
    String name();

    boolean applicable(PromotionContext ctx);

    BigDecimal discount(PromotionContext ctx);
}
