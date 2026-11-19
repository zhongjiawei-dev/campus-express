package com.campus.express.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),

    PARAM_ERROR(10001, "参数错误"),
    UNAUTHORIZED(10002, "未登录或登录已过期"),
    FORBIDDEN(10003, "无权限"),
    RATE_LIMIT(10004, "请求过于频繁"),
    SYSTEM_BUSY(10005, "系统繁忙"),

    USER_NOT_FOUND(20001, "用户不存在"),
    USER_DISABLED(20002, "账号已被禁用"),
    NOT_CERTIFIED(20003, "未完成代取员认证"),
    BALANCE_NOT_ENOUGH(20004, "余额不足"),

    ORDER_NOT_FOUND(30001, "订单不存在"),
    ORDER_STATE_ILLEGAL(30002, "订单状态不允许该操作"),
    ORDER_STATE_CONFLICT(30003, "订单状态已变化，请刷新重试"),
    ORDER_ALREADY_ACCEPTED(30004, "订单已被接单"),
    ORDER_TIMEOUT(30005, "订单已超时关闭"),
    PICKUP_CODE_ERROR(30006, "取件码错误"),
    CAPACITY_EXCEEDED(30007, "超出接单能力"),

    PAY_FAILED(40001, "支付失败"),
    PAY_SIGN_ERROR(40002, "支付验签失败"),
    PAY_AMOUNT_MISMATCH(40003, "支付金额不一致"),
    PAY_DUPLICATED(40004, "重复支付"),

    WITHDRAW_BELOW_MIN(50001, "低于最低提现金额"),
    WITHDRAW_LIMIT(50002, "超出提现限额"),
    WITHDRAW_NAME_MISMATCH(50003, "实名信息不一致"),

    CONTENT_SENSITIVE(60001, "内容包含敏感词"),

    SYSTEM_ERROR(90001, "系统异常，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
