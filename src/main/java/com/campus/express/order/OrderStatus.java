package com.campus.express.order;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum OrderStatus {

    PENDING_PAY(10, "待支付"),
    WAIT_ACCEPT(20, "待接单"),
    ACCEPTED(30, "已接单"),
    PICKED(40, "已取件"),
    DELIVERED(50, "已送达"),
    COMPLETED(60, "已完成"),
    CANCELLED(70, "已取消"),
    REFUNDING(80, "退款中"),
    REFUNDED(90, "已退款"),
    AFTER_SALE(100, "售后处理中");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static String descOf(Integer code) {
        if (code == null) {
            return "";
        }
        return Arrays.stream(values())
                .filter(s -> s.code == code)
                .map(OrderStatus::getDesc)
                .findFirst()
                .orElse("未知状态");
    }

    public static final List<Integer> ACTIVE_COURIER_STATUS = List.of(30, 40, 50);
}
