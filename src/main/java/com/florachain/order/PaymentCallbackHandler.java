package com.florachain.order;

import com.florachain.payment.PaymentListener;

import java.math.BigDecimal;
import java.util.Optional;

/** 支付成功后的订单处理：用 CAS 保证重复回调只生效一次（FR-O04）。 */
public class PaymentCallbackHandler implements PaymentListener {
    private final OrderRepository repository;

    public PaymentCallbackHandler(OrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void onPaid(String orderNo, String txnNo, BigDecimal amount) {
        Optional<Order> order = repository.find(orderNo);
        if (order.isEmpty()) {
            throw new IllegalStateException("订单不存在：" + orderNo);
        }
        // 订单已被超时关闭时 CAS 失败，需要走退款流程，这里只返回，由对账任务处理
        order.get().transit(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, "payment-callback");
    }
}
