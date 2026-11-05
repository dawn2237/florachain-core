package com.florachain.payment;

import java.math.BigDecimal;

public record PayRequest(String orderNo, BigDecimal amount, String subject) {
}
