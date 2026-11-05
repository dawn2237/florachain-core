package com.florachain.order;

import com.florachain.common.BusinessException;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.florachain.order.OrderStatus.*;

/** 订单状态机：集中定义合法流转，避免在业务代码里散落 if 判断。 */
public final class OrderStateMachine {
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        put(PENDING_PAYMENT, PAID, CANCELLED);
        put(PAID, PREPARING, CANCELLED);          // 已支付后退款视为取消
        put(PREPARING, DELIVERING, AFTER_SALE);
        put(DELIVERING, COMPLETED, AFTER_SALE);
        put(COMPLETED, AFTER_SALE);
        put(AFTER_SALE, COMPLETED, CANCELLED);    // 售后处理完毕可回到已完成或转为取消（缺陷 BUG-011）
        put(CANCELLED);
    }

    private OrderStateMachine() {
    }

    private static void put(OrderStatus from, OrderStatus... to) {
        Set<OrderStatus> set = EnumSet.noneOf(OrderStatus.class);
        set.addAll(Arrays.asList(to));
        TRANSITIONS.put(from, set);
    }

    public static boolean canTransit(OrderStatus from, OrderStatus to) {
        return TRANSITIONS.get(from).contains(to);
    }

    public static void assertTransit(OrderStatus from, OrderStatus to) {
        if (!canTransit(from, to)) {
            throw new BusinessException(4001, "订单状态不允许从[" + from.label() + "]变为[" + to.label() + "]");
        }
    }
}
