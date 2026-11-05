package com.florachain.inventory;

import java.time.LocalDate;

/** 保鲜期评估策略接口（FR-S05），不同花材可以配置不同的阈值。 */
public interface ExpiryPolicy {
    ExpiryLevel evaluate(FlowerBatch batch, LocalDate today);
}
