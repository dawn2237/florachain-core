package com.florachain.promotion;

import java.math.BigDecimal;

public record PromotionContext(BigDecimal amount, String couponCode, boolean flashSaleItem) {
}
