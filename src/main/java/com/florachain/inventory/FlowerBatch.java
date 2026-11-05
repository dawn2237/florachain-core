package com.florachain.inventory;

import java.time.LocalDate;

/** 花材批次：同一供应商同一次到货，有统一的到货日期与保鲜期。 */
public record FlowerBatch(String batchId, String materialId, String supplierId,
                          LocalDate arrivalDate, int shelfLifeDays, int remaining) {
    public LocalDate expireDate() {
        return arrivalDate.plusDays(shelfLifeDays);
    }
}
