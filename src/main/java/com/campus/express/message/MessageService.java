package com.campus.express.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.message.mapper.MessageMapper;
import com.campus.express.order.Order;
import com.campus.express.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    public void send(Long userId, String title, String content, String bizNo) {
        Message message = new Message();
        message.setUserId(userId);
        message.setMsgType(1);
        message.setTitle(title);
        message.setContent(content);
        message.setBizNo(bizNo);
        message.setIsRead(0);
        message.setChannel(1);
        message.setSendStatus(2);
        messageMapper.insert(message);
        // 生产环境在此接入短信 / 公众号模板消息通道，失败只记录不影响主流程
        log.info("message sent userId={} title={} bizNo={}", userId, title, bizNo);
    }

    public List<Message> list(Long userId, int page, int size) {
        Page<Message> result = messageMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .orderByDesc(Message::getCreatedAt));
        return result.getRecords();
    }

    public long unreadCount(Long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getIsRead, 0));
    }

    public void markRead(Long userId, Long id) {
        messageMapper.markRead(id, userId);
    }

    public void notifyStatus(Order order, int status) {
        String title;
        String content;
        switch (status) {
            case 20 -> {
                title = "支付成功";
                content = "订单 " + order.getOrderNo() + " 已支付，正在为你匹配代取员。";
            }
            case 30 -> {
                title = "代取员已接单";
                content = "订单 " + order.getOrderNo() + " 已有代取员接单，请留意取件码与取件时间。";
            }
            case 40 -> {
                title = "包裹已取件";
                content = "代取员已完成取件，正在送往你的收货地址。";
            }
            case 50 -> {
                title = "包裹已送达";
                content = "包裹已送达，请及时确认收货；如有问题可发起售后。";
            }
            case 60 -> {
                title = "订单已完成";
                content = "订单 " + order.getOrderNo() + " 已完成，感谢使用校园代取。";
            }
            case 70 -> {
                title = "订单已取消";
                content = "订单 " + order.getOrderNo() + " 已取消。";
            }
            case 80 -> {
                title = "退款处理中";
                content = "订单 " + order.getOrderNo() + " 的退款申请已受理，预计 1-3 个工作日到账。";
            }
            case 90 -> {
                title = "退款已到账";
                content = "订单 " + order.getOrderNo() + " 的退款已退回钱包余额。";
            }
            case 100 -> {
                title = "售后处理中";
                content = "订单 " + order.getOrderNo() + " 已进入售后流程，平台将在 24 小时内联系你。";
            }
            default -> {
                return;
            }
        }
        send(order.getUserId(), title, content, order.getOrderNo());
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    public String statusText(Integer status) {
        return OrderStatus.descOf(status);
    }
}
