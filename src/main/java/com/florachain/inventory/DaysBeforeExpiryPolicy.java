package com.florachain.inventory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DaysBeforeExpiryPolicy implements ExpiryPolicy {
    private final int warningDays;
    private final int criticalDays;

    public DaysBeforeExpiryPolicy(int warningDays, int criticalDays) {
        this.warningDays = warningDays;
        this.criticalDays = criticalDays;
    }

    @Override
    public ExpiryLevel evaluate(FlowerBatch batch, LocalDate today) {
        long left = ChronoUnit.DAYS.between(today, batch.expireDate());
        if (left < 0) {
            return ExpiryLevel.EXPIRED;
        }
        if (left <= criticalDays) {
            return ExpiryLevel.CRITICAL;
        }
        if (left <= warningDays) {
            return ExpiryLevel.WARNING;
        }
        return ExpiryLevel.NORMAL;
    }
}
