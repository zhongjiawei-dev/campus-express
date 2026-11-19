package com.campus.express.order.grab;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;
import com.campus.express.courier.Courier;
import com.campus.express.courier.CourierService;
import com.campus.express.order.Operator;
import com.campus.express.order.Order;
import com.campus.express.order.OrderEvent;
import com.campus.express.order.OrderStateService;
import com.campus.express.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderGrabService {

    private final OrderStateService stateService;
    private final CourierService courierService;
    private final GrabLock grabLock;
    private final OrderMapper orderMapper;

    /**
     * 抢单三重防线：Redis/内存锁挡瞬时并发 → 接单能力校验挡无效点击 →
     * 数据库状态 CAS 兜底，保证一单只能被一个代取员接走。
     */
    public Order grab(String orderNo, Long userId) {
        Courier courier = courierService.requireApproved(userId);
        Order order = stateService.findByNo(orderNo);

        if (order.getCourierId() != null && order.getCourierId().equals(courier.getId())) {
            return order;
        }
        if (order.getStatus() != 20) {
            throw new BizException(ErrorCode.ORDER_ALREADY_ACCEPTED);
        }
        checkServiceWindow(courier);
        if (order.getWeightGram() != null && order.getWeightGram() > courier.getMaxWeightGram()) {
            throw new BizException(ErrorCode.CAPACITY_EXCEEDED, "超出你的可接重量");
        }
        long active = orderMapper.countActiveByCourier(courier.getId());
        if (active >= levelLimit(courier.getLevel())) {
            throw new BizException(ErrorCode.CAPACITY_EXCEEDED, "同时进行中的订单已达上限");
        }

        int lock = grabLock.tryLock(orderNo, courier.getId());
        if (lock == GrabLock.HELD_BY_OTHER) {
            throw new BizException(ErrorCode.ORDER_ALREADY_ACCEPTED);
        }
        if (lock == GrabLock.HELD_BY_SELF) {
            return order;
        }

        try {
            Order updated = stateService.fire(orderNo, OrderEvent.ACCEPT,
                    Operator.courier(userId, courier.getId()), "抢单成功");
            log.info("grab success orderNo={} courierId={}", orderNo, courier.getId());
            return updated;
        } catch (RuntimeException e) {
            grabLock.unlock(orderNo, courier.getId());
            throw e;
        }
    }

    private void checkServiceWindow(Courier courier) {
        String start = courier.getServiceStart();
        String end = courier.getServiceEnd();
        if (start == null || end == null) {
            return;
        }
        LocalTime now = LocalTime.now();
        LocalTime startTime = LocalTime.parse(start);
        LocalTime endTime = LocalTime.parse(end);
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            throw new BizException(ErrorCode.CAPACITY_EXCEEDED,
                    "当前不在你的服务时段（" + start + "-" + end + "）");
        }
    }

    private int levelLimit(Integer level) {
        if (level == null) {
            return 2;
        }
        return switch (level) {
            case 2 -> 3;
            case 3 -> 5;
            default -> 2;
        };
    }
}
