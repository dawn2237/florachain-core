package com.florachain.payment;

import java.math.BigDecimal;
import java.util.Map;

/** 微信支付渠道（沙箱模拟）：金额单位为分。 */
public class WechatPayChannel extends AbstractPaymentChannel {
    public WechatPayChannel(String secret, IdempotencyStore idempotency, PaymentListener listener) {
        super(secret, idempotency, listener);
    }

    @Override
    public String name() { return "wechat"; }

    @Override
    public PayResult pay(PayRequest request) {
        return new PayResult(true, "WX" + request.orderNo(), "沙箱下单成功");
    }

    @Override
    public PayResult refund(String orderNo, BigDecimal amount) {
        return new PayResult(true, "WXR" + orderNo, "沙箱退款成功");
    }

    @Override
    protected boolean isSuccess(Map<String, String> p) { return "SUCCESS".equals(p.get("result_code")); }

    @Override
    protected String orderNoOf(Map<String, String> p) { return p.get("out_trade_no"); }

    @Override
    protected String txnNoOf(Map<String, String> p) { return p.get("transaction_id"); }

    @Override
    protected BigDecimal amountOf(Map<String, String> p) {
        return new BigDecimal(p.get("total_fee")).movePointLeft(2);
    }
}
