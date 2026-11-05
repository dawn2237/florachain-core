package com.florachain.inventory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** 扫描批次并给出预警：严重和过期的批次提示促销或报损。 */
public class ExpiryWarningService {
    public record Warning(FlowerBatch batch, ExpiryLevel level) {
    }

    private final Function<FlowerBatch, ExpiryPolicy> policyOf;

    public ExpiryWarningService(Map<String, ExpiryPolicy> policyByMaterial, ExpiryPolicy defaultPolicy) {
        this.policyOf = b -> policyByMaterial.getOrDefault(b.materialId(), defaultPolicy);
    }

    public List<Warning> scan(List<FlowerBatch> batches, LocalDate today) {
        return batches.stream()
                .filter(b -> b.remaining() > 0)
                .map(b -> new Warning(b, policyOf.apply(b).evaluate(b, today)))
                .filter(w -> w.level() != ExpiryLevel.NORMAL)
                .toList();
    }
}
