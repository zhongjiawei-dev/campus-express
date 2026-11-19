package com.campus.express.order;

public enum OrderEvent {

    PAY("支付成功"),
    ACCEPT("代取员接单"),
    PICK("确认取件"),
    DELIVER("确认送达"),
    CONFIRM("用户确认收货"),
    CANCEL_BY_USER("用户取消"),
    CANCEL("取消订单"),
    REFUND_SUCCESS("退款完成"),
    AFTER_SALE("发起售后"),
    RESOLVE("仲裁完成");

    private final String desc;

    OrderEvent(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
