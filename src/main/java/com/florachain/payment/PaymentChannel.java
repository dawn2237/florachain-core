package com.florachain.payment;

import java.util.Map;

/** 支付渠道接口（FR-O03）。 */
public interface PaymentChannel {
    String name();

    PayResult pay(PayRequest request);

    PayResult refund(String orderNo, java.math.BigDecimal amount);

    CallbackResult handleCallback(Map<String, String> params);
}
