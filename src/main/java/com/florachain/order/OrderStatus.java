package com.florachain.order;

/** 订单状态（FR-O02）。 */
public enum OrderStatus {
    PENDING_PAYMENT("待支付"), PAID("已支付"), PREPARING("备货中"), DELIVERING("配送中"),
    COMPLETED("已完成"), CANCELLED("已取消"), AFTER_SALE("售后中");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
