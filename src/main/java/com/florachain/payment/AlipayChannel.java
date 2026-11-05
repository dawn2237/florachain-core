package com.florachain.payment;

import java.math.BigDecimal;
import java.util.Map;

/** 支付宝渠道（沙箱模拟）：金额单位为元。 */
public class AlipayChannel extends AbstractPaymentChannel {
    public AlipayChannel(String secret, IdempotencyStore idempotency, PaymentListener listener) {
        super(secret, idempotency, listener);
    }

    @Override
    public String name() { return "alipay"; }

    @Override
    public PayResult pay(PayRequest request) {
        return new PayResult(true, "ALI" + request.orderNo(), "沙箱下单成功");
    }

    @Override
    public PayResult refund(String orderNo, BigDecimal amount) {
        return new PayResult(true, "ALIR" + orderNo, "沙箱退款成功");
    }

    @Override
    protected boolean isSuccess(Map<String, String> p) { return "TRADE_SUCCESS".equals(p.get("trade_status")); }

    @Override
    protected String orderNoOf(Map<String, String> p) { return p.get("out_trade_no"); }

    @Override
    protected String txnNoOf(Map<String, String> p) { return p.get("trade_no"); }

    @Override
    protected BigDecimal amountOf(Map<String, String> p) { return new BigDecimal(p.get("total_amount")); }
}
