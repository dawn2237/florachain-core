package com.florachain.payment;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 模板方法：回调处理的顺序固定为 验签 → 判断状态 → 幂等登记 → 通知订单模块（FR-O04）。
 * 各渠道只需说明如何取字段，避免每个渠道各写一遍易出错的流程。
 */
public abstract class AbstractPaymentChannel implements PaymentChannel {
    private final String secret;
    private final IdempotencyStore idempotency;
    private final PaymentListener listener;

    protected AbstractPaymentChannel(String secret, IdempotencyStore idempotency, PaymentListener listener) {
        this.secret = secret;
        this.idempotency = idempotency;
        this.listener = listener;
    }

    @Override
    public final CallbackResult handleCallback(Map<String, String> params) {
        if (!verifySignature(params)) {
            return CallbackResult.rejected("签名校验失败");
        }
        if (!isSuccess(params)) {
            return CallbackResult.ignored("非支付成功状态");
        }
        String orderNo = orderNoOf(params);
        String txnNo = txnNoOf(params);
        if (!idempotency.markIfAbsent(name() + ":" + txnNo)) {
            return CallbackResult.duplicate(orderNo);
        }
        listener.onPaid(orderNo, txnNo, amountOf(params));
        return CallbackResult.accepted(orderNo);
    }

    protected abstract boolean isSuccess(Map<String, String> params);

    protected abstract String orderNoOf(Map<String, String> params);

    protected abstract String txnNoOf(Map<String, String> params);

    protected abstract BigDecimal amountOf(Map<String, String> params);

    /** 对除 sign 以外的参数按字典序拼接后做 HMAC-SHA256。 */
    public String sign(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(params).forEach((k, v) -> {
            if (!"sign".equals(k)) {
                sb.append(k).append('=').append(v).append('&');
            }
        });
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean verifySignature(Map<String, String> params) {
        String given = params.get("sign");
        return given != null && given.equals(sign(params));
    }
}
