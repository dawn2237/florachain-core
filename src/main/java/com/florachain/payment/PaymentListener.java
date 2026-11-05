package com.florachain.payment;

import java.math.BigDecimal;

/** 观察者：支付渠道确认到账后通知订单模块。 */
@FunctionalInterface
public interface PaymentListener {
    void onPaid(String orderNo, String txnNo, BigDecimal amount);
}
