package com.florachain.delivery;

import java.math.BigDecimal;

public record DeliveryContext(double distanceKm, double weightKg, BigDecimal orderAmount, String area) {
}
