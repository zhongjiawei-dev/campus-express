package com.campus.express.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;
import com.campus.express.common.LoginUser;
import com.campus.express.message.MessageService;
import com.campus.express.order.mapper.OrderMapper;
import com.campus.express.order.mapper.OrderStatusLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单状态流转执行器：校验 → 状态机裁决 → CAS 更新 → 写流水 → 发通知。
 * 所有流转必须走这里，避免散落各处的裸 UPDATE。
 */
@Service
@RequiredArgsConstructor
public class OrderStateService {

    private final OrderMapper orderMapper;
    private final OrderStatusLogMapper logMapper;
    private final MessageService messageService;

    @Transactional
    public Order fire(String orderNo, OrderEvent event, Operator operator, String remark) {
        Order order = findByNo(orderNo);
        int from = order.getStatus();
        guard(order, event, operator);

        int to = OrderStateMachine.next(from, event);
        int rows = orderMapper.casUpdateStatus(orderNo, from, to, operator.courierId());
        if (rows != 1) {
            throw new BizException(ErrorCode.ORDER_STATE_CONFLICT);
        }

        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderNo(orderNo);
        statusLog.setFromStatus(from);
        statusLog.setToStatus(to);
        statusLog.setEvent(event.name());
        statusLog.setOperatorId(operator.accountId());
        statusLog.setOperatorRole(operator.role());
        statusLog.setRemark(remark);
        logMapper.insert(statusLog);

        messageService.notifyStatus(order, to);
        return findByNo(orderNo);
    }

    public Order findByNo(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private void guard(Order order, OrderEvent event, Operator operator) {
        switch (event) {
            case ACCEPT -> {
                if (operator.courierId() == null) {
                    throw new BizException(ErrorCode.FORBIDDEN, "仅认证代取员可接单");
                }
                if (order.getCourierId() != null) {
                    throw new BizException(ErrorCode.ORDER_ALREADY_ACCEPTED);
                }
            }
            case PICK, DELIVER -> {
                if (order.getCourierId() == null || !order.getCourierId().equals(operator.courierId())) {
                    throw new BizException(ErrorCode.FORBIDDEN, "只能操作自己接的订单");
                }
                if (event == OrderEvent.PICK && order.getPickupCodeVerified() != null
                        && order.getPickupCodeVerified() != 1) {
                    throw new BizException(ErrorCode.PICKUP_CODE_ERROR, "请先完成取件码核验");
                }
                if (event == OrderEvent.DELIVER && order.getPhotoDeliverUrl() == null) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "请先上传送达凭证");
                }
            }
            case PAY, CONFIRM, CANCEL_BY_USER, AFTER_SALE -> {
                if (!order.getUserId().equals(operator.accountId())) {
                    throw new BizException(ErrorCode.FORBIDDEN, "只能操作自己的订单");
                }
            }
            case CANCEL -> {
                boolean isOwner = order.getUserId().equals(operator.accountId());
                boolean isAdmin = LoginUser.ADMIN.equals(operator.role());
                if (!isOwner && !isAdmin) {
                    throw new BizException(ErrorCode.FORBIDDEN, "无权取消该订单");
                }
            }
            default -> {
            }
        }
    }
}
