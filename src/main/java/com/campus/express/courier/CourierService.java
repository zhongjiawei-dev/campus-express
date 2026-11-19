package com.campus.express.courier;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;
import com.campus.express.common.MaskUtil;
import com.campus.express.courier.mapper.CourierMapper;
import com.campus.express.order.mapper.OrderMapper;
import com.campus.express.wallet.Wallet;
import com.campus.express.wallet.WalletFlow;
import com.campus.express.wallet.WalletService;
import com.campus.express.wallet.mapper.WalletFlowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CourierService {

    private final CourierMapper courierMapper;
    private final OrderMapper orderMapper;
    private final WalletService walletService;
    private final WalletFlowMapper flowMapper;

    public Courier findByUser(Long userId) {
        return courierMapper.selectOne(new LambdaQueryWrapper<Courier>().eq(Courier::getUserId, userId));
    }

    public Courier byId(Long courierId) {
        return courierMapper.selectById(courierId);
    }

    public void increaseFinished(Long courierId) {
        courierMapper.increaseFinished(courierId);
    }

    public Courier requireApproved(Long userId) {
        Courier courier = findByUser(userId);
        if (courier == null) {
            throw new BizException(ErrorCode.NOT_CERTIFIED, "请先提交代取员实名认证");
        }
        if (courier.getAuditStatus() == null || courier.getAuditStatus() != Courier.AUDIT_APPROVED) {
            throw new BizException(ErrorCode.NOT_CERTIFIED, "实名认证尚未通过");
        }
        return courier;
    }

    @Transactional
    public Courier apply(Long userId, ApplyRequest request) {
        if (request.realName() == null || request.realName().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请填写真实姓名");
        }
        if (request.studentCardUrl() == null || request.studentCardUrl().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请上传学生证照片");
        }
        Courier courier = findByUser(userId);
        boolean isNew = courier == null;
        if (isNew) {
            courier = new Courier();
            courier.setUserId(userId);
            courier.setAuditStatus(Courier.AUDIT_PENDING);
            courier.setWorkStatus(1);
            courier.setLevel(1);
            courier.setScore(new BigDecimal("5.00"));
            courier.setFinishedCnt(0);
            courier.setViolationCnt(0);
        }
        courier.setRealName(request.realName());
        courier.setStudentNo(request.studentNo());
        courier.setIdCardMask(MaskUtil.idCard(request.idCard()));
        courier.setStudentCardUrl(request.studentCardUrl());
        courier.setServiceStart(request.serviceStart() == null ? "08:00" : request.serviceStart());
        courier.setServiceEnd(request.serviceEnd() == null ? "20:00" : request.serviceEnd());
        courier.setMaxWeightGram(request.maxWeightGram() == null ? 5000 : request.maxWeightGram());
        courier.setStationIds(request.stationIds());
        courier.setAuditStatus(Courier.AUDIT_PENDING);
        courier.setAuditRemark("资料已提交，等待平台审核");
        if (isNew) {
            courierMapper.insert(courier);
        } else {
            courierMapper.updateById(courier);
        }
        return courier;
    }

    @Transactional
    public void updateSettings(Long userId, Integer workStatus, String start, String end,
                               Integer maxWeightGram, String stationIds) {
        Courier courier = requireApproved(userId);
        if (workStatus != null) {
            courier.setWorkStatus(workStatus);
        }
        if (start != null) {
            courier.setServiceStart(start);
        }
        if (end != null) {
            courier.setServiceEnd(end);
        }
        if (maxWeightGram != null) {
            courier.setMaxWeightGram(maxWeightGram);
        }
        if (stationIds != null) {
            courier.setStationIds(stationIds);
        }
        courierMapper.updateById(courier);
    }

    public Map<String, Object> profile(Long userId) {
        Courier courier = findByUser(userId);
        Wallet wallet = walletService.overview(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courier", courier);
        result.put("wallet", wallet);
        result.put("todayIncome", flowMapper.incomeSince(userId, LocalDate.now().atStartOfDay()));
        result.put("totalIncome", wallet.getTotalIncome());
        if (courier != null) {
            result.put("activeOrders", orderMapper.countActiveByCourier(courier.getId()));
        }
        return result;
    }

    public List<WalletFlow> incomeFlows(Long userId, int page, int size) {
        return walletService.flows(userId, page, size);
    }

    @Transactional
    public void audit(Long courierId, boolean pass, String remark) {
        Courier courier = courierMapper.selectById(courierId);
        if (courier == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "代取员不存在");
        }
        courier.setAuditStatus(pass ? Courier.AUDIT_APPROVED : Courier.AUDIT_REJECTED);
        courier.setAuditRemark(remark == null ? (pass ? "审核通过" : "资料不符合要求") : remark);
        courierMapper.updateById(courier);
    }

    public record ApplyRequest(String realName, String studentNo, String idCard, String studentCardUrl,
                               String serviceStart, String serviceEnd, Integer maxWeightGram, String stationIds) {
    }
}
