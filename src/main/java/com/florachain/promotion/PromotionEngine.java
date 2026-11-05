package com.florachain.promotion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** 从适用的活动中选出优惠最大的一个（本期不叠加）。 */
public class PromotionEngine {
    public record Applied(String name, BigDecimal discount) {
    }

    private final List<PromotionStrategy> strategies;

    public PromotionEngine(List<PromotionStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public Optional<Applied> best(PromotionContext ctx) {
        return strategies.stream()
                .filter(s -> s.applicable(ctx))
                .map(s -> new Applied(s.name(), s.discount(ctx).min(ctx.amount())))
                .max((a, b) -> a.discount().compareTo(b.discount()));
    }
}
