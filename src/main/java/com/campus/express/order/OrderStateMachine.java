package com.campus.express.order;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;

import java.util.Arrays;
import java.util.Map;

/**
 * 标准订单状态机：状态流转的唯一裁决入口。
 * 所有订单动作必须先经过本类校验，再执行带状态条件的 CAS 更新。
 */
public final class OrderStateMachine {

    private OrderStateMachine() {
    }

    public static int next(int from, OrderEvent event) {
        switch (event) {
            case PAY:
                expect(from, event, OrderStatus.PENDING_PAY.getCode());
                return OrderStatus.WAIT_ACCEPT.getCode();
            case ACCEPT:
                expect(from, event, OrderStatus.WAIT_ACCEPT.getCode());
                return OrderStatus.ACCEPTED.getCode();
            case PICK:
                expect(from, event, OrderStatus.ACCEPTED.getCode());
                return OrderStatus.PICKED.getCode();
            case DELIVER:
                expect(from, event, OrderStatus.PICKED.getCode());
                return OrderStatus.DELIVERED.getCode();
            case CONFIRM:
                expect(from, event, OrderStatus.DELIVERED.getCode());
                return OrderStatus.COMPLETED.getCode();
            case CANCEL_BY_USER:
                expect(from, event, OrderStatus.PENDING_PAY.getCode(), OrderStatus.WAIT_ACCEPT.getCode());
                return OrderStatus.CANCELLED.getCode();
            case CANCEL:
                if (from == OrderStatus.ACCEPTED.getCode()) {
                    return OrderStatus.REFUNDING.getCode();
                }
                expect(from, event, OrderStatus.WAIT_ACCEPT.getCode(), OrderStatus.PENDING_PAY.getCode());
                return OrderStatus.CANCELLED.getCode();
            case REFUND_SUCCESS:
                expect(from, event, OrderStatus.REFUNDING.getCode());
                return OrderStatus.REFUNDED.getCode();
            case AFTER_SALE:
                expect(from, event,
                        OrderStatus.PICKED.getCode(),
                        OrderStatus.DELIVERED.getCode(),
                        OrderStatus.COMPLETED.getCode());
                return OrderStatus.AFTER_SALE.getCode();
            case RESOLVE:
                expect(from, event, OrderStatus.AFTER_SALE.getCode());
                return OrderStatus.COMPLETED.getCode();
            default:
                throw new BizException(ErrorCode.ORDER_STATE_ILLEGAL, "未定义的状态事件");
        }
    }

    public static boolean isTerminal(int status) {
        return status == OrderStatus.COMPLETED.getCode()
                || status == OrderStatus.CANCELLED.getCode()
                || status == OrderStatus.REFUNDED.getCode();
    }

    private static void expect(int from, OrderEvent event, int... allowed) {
        boolean matched = Arrays.stream(allowed).anyMatch(code -> code == from);
        if (!matched) {
            throw new BizException(ErrorCode.ORDER_STATE_ILLEGAL,
                    "订单当前状态为「" + OrderStatus.descOf(from) + "」，不能执行「" + event.getDesc() + "」");
        }
    }

    /** 供测试与前端展示：全部合法迁移 */
    public static Map<Integer, Integer> transitions(OrderEvent event) {
        return switch (event) {
            case PAY -> Map.of(10, 20);
            case ACCEPT -> Map.of(20, 30);
            case PICK -> Map.of(30, 40);
            case DELIVER -> Map.of(40, 50);
            case CONFIRM -> Map.of(50, 60);
            case CANCEL_BY_USER -> Map.of(10, 70, 20, 70);
            case CANCEL -> Map.of(20, 70, 30, 80);
            case REFUND_SUCCESS -> Map.of(80, 90);
            case AFTER_SALE -> Map.of(40, 100, 50, 100, 60, 100);
            case RESOLVE -> Map.of(100, 60);
        };
    }
}
