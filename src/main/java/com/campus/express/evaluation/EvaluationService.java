package com.campus.express.evaluation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;
import com.campus.express.common.SensitiveWordFilter;
import com.campus.express.evaluation.mapper.EvaluationMapper;
import com.campus.express.order.Order;
import com.campus.express.order.OrderStatus;
import com.campus.express.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final OrderMapper orderMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Transactional
    public Evaluation submit(Long userId, String orderNo, Integer score, String tags,
                             String content, Boolean anonymous) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "订单号不能为空");
        }
        if (score == null || score < 1 || score > 5) {
            throw new BizException(ErrorCode.PARAM_ERROR, "评分需在 1-5 星之间");
        }
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (order.getStatus() != OrderStatus.COMPLETED.getCode()) {
            throw new BizException(ErrorCode.ORDER_STATE_ILLEGAL, "订单完成后才能评价");
        }
        Evaluation exists = evaluationMapper.selectOne(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getOrderNo, orderNo));
        if (exists != null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "该订单已评价");
        }

        Evaluation evaluation = new Evaluation();
        evaluation.setOrderNo(orderNo);
        evaluation.setUserId(userId);
        evaluation.setCourierId(order.getCourierId());
        evaluation.setScore(score);
        evaluation.setTags(sensitiveWordFilter.sanitize(tags));
        evaluation.setContent(sensitiveWordFilter.sanitize(content));
        evaluation.setAnonymous(Boolean.TRUE.equals(anonymous) ? 1 : 0);
        evaluation.setStatus(1);
        evaluation.setCreatedAt(LocalDateTime.now());
        evaluationMapper.insert(evaluation);
        return evaluation;
    }

    public Evaluation findByOrder(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return null;
        }
        return evaluationMapper.selectOne(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getOrderNo, orderNo)
                .eq(Evaluation::getStatus, 1));
    }
}
