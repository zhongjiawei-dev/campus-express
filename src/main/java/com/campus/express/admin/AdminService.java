package com.campus.express.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;
import com.campus.express.common.JwtUtil;
import com.campus.express.common.LoginUser;
import com.campus.express.common.MaskUtil;
import com.campus.express.complaint.Complaint;
import com.campus.express.complaint.mapper.ComplaintMapper;
import com.campus.express.courier.Courier;
import com.campus.express.courier.mapper.CourierMapper;
import com.campus.express.message.MessageService;
import com.campus.express.order.Order;
import com.campus.express.order.OrderService;
import com.campus.express.order.OrderStatus;
import com.campus.express.order.mapper.OrderMapper;
import com.campus.express.pricing.PriceRule;
import com.campus.express.pricing.mapper.PriceRuleMapper;
import com.campus.express.user.User;
import com.campus.express.user.mapper.UserMapper;
import com.campus.express.wallet.WalletService;
import com.campus.express.wallet.Withdraw;
import com.campus.express.wallet.mapper.WithdrawMapper;
import com.campus.express.admin.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminMapper adminMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final CourierMapper courierMapper;
    private final WithdrawMapper withdrawMapper;
    private final ComplaintMapper complaintMapper;
    private final PriceRuleMapper priceRuleMapper;
    private final WalletService walletService;
    private final MessageService messageService;
    private final OrderService orderService;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(String username, String password) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username));
        if (admin == null || !passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() != 1) {
            throw new BizException(ErrorCode.FORBIDDEN, "管理员账号已被停用");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtUtil.generate(admin.getId(), LoginUser.ADMIN));
        result.put("username", admin.getUsername());
        result.put("realName", admin.getRealName());
        result.put("role", LoginUser.ADMIN);
        return result;
    }

    public Map<String, Object> dashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayOrders", orderMapper.countSince(todayStart));
        result.put("todayGmv", orderMapper.gmvSince(todayStart));
        result.put("weekGmv", orderMapper.gmvSince(weekStart));
        result.put("activeUsers", orderMapper.activeUsersSince(weekStart));
        result.put("statusCounts", orderMapper.countByStatus());
        result.put("topCouriers", orderMapper.topCouriers(weekStart));
        result.put("pendingAudit", courierMapper.selectCount(new LambdaQueryWrapper<Courier>()
                .eq(Courier::getAuditStatus, Courier.AUDIT_PENDING)));
        result.put("pendingWithdraw", withdrawMapper.selectCount(new LambdaQueryWrapper<Withdraw>()
                .eq(Withdraw::getStatus, 1)));
        result.put("openComplaints", complaintMapper.selectCount(new LambdaQueryWrapper<Complaint>()
                .eq(Complaint::getStatus, 0)));
        result.put("pendingOrders", orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.WAIT_ACCEPT.getCode())));
        return result;
    }

    public Page<Order> orders(String status, String keyword, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Order::getStatus, Integer.parseInt(status));
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Order::getOrderNo, keyword);
        }
        wrapper.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public IPage<Map<String, Object>> users(int page, int size) {
        Page<User> result = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
        return result.convert(user -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", user.getId());
            view.put("nickname", user.getNickname());
            view.put("phoneMask", user.getPhoneMask());
            view.put("studentNo", user.getStudentNo());
            view.put("status", user.getStatus());
            view.put("creditScore", user.getCreditScore());
            view.put("createdAt", user.getCreatedAt());
            return view;
        });
    }

    public Page<Courier> couriers(Integer auditStatus, int page, int size) {
        LambdaQueryWrapper<Courier> wrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null) {
            wrapper.eq(Courier::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(Courier::getCreatedAt);
        return courierMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public List<PriceRule> priceRules() {
        return priceRuleMapper.selectList(new LambdaQueryWrapper<PriceRule>()
                .eq(PriceRule::getStatus, 1)
                .orderByAsc(PriceRule::getOrderType));
    }

    @Transactional
    public PriceRule savePriceRule(PriceRule rule) {
        if (rule.getOrderType() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择快递类型");
        }
        if (rule.getId() != null) {
            PriceRule existed = priceRuleMapper.selectById(rule.getId());
            if (existed == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "计价规则不存在");
            }
            rule.setVersion(existed.getVersion() == null ? 2 : existed.getVersion() + 1);
        } else {
            rule.setVersion(1);
        }
        if (rule.getRuleNo() == null || rule.getRuleNo().isBlank()) {
            rule.setRuleNo("R" + System.currentTimeMillis());
        }
        rule.setStatus(1);
        if (rule.getId() == null) {
            priceRuleMapper.insert(rule);
        } else {
            priceRuleMapper.updateById(rule);
        }
        return rule;
    }

    public Page<Withdraw> withdraws(Integer status, int page, int size) {
        LambdaQueryWrapper<Withdraw> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Withdraw::getStatus, status);
        }
        wrapper.orderByDesc(Withdraw::getCreatedAt);
        return withdrawMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void auditWithdraw(Long id, boolean pass, String remark, Long adminId) {
        walletService.auditWithdraw(id, pass, remark, adminId);
    }

    public Page<Complaint> complaints(Integer status, int page, int size) {
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Complaint::getStatus, status);
        }
        wrapper.orderByDesc(Complaint::getCreatedAt);
        return complaintMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public Complaint resolveComplaint(Long id, String result, BigDecimal compensation,
                                      Integer responsibility, Long adminId) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "投诉单不存在");
        }
        complaint.setStatus(2);
        complaint.setResult(result);
        complaint.setCompensation(compensation == null ? BigDecimal.ZERO : compensation);
        complaint.setResponsibility(responsibility);
        complaint.setHandlerId(adminId);
        complaint.setCloseTime(LocalDateTime.now());
        complaintMapper.updateById(complaint);

        orderService.closeAfterSale(complaint.getOrderNo(), adminId);

        if (complaint.getCompensation().signum() > 0 && complaint.getUserId() != null) {
            walletService.credit(complaint.getUserId(), complaint.getCompensation(), 7,
                    complaint.getComplaintNo(), "投诉赔付");
        }
        if (responsibility != null && responsibility == 2 && complaint.getCourierId() != null) {
            courierMapper.increaseViolation(complaint.getCourierId());
        }
        if (complaint.getUserId() != null) {
            messageService.send(complaint.getUserId(), "售后处理完成",
                    "投诉 " + complaint.getComplaintNo() + " 已处理：" + result, complaint.getOrderNo());
        }
        return complaint;
    }

    @Transactional
    public void broadcast(String title, String content) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 0)
                .last("LIMIT 200"));
        for (User user : users) {
            messageService.send(user.getId(), title, content, null);
        }
    }

    public Map<String, Object> maskedCourier(Courier courier) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", courier.getId());
        view.put("realName", MaskUtil.name(courier.getRealName()));
        view.put("studentNo", courier.getStudentNo());
        view.put("idCardMask", courier.getIdCardMask());
        view.put("auditStatus", courier.getAuditStatus());
        view.put("score", courier.getScore());
        view.put("finishedCnt", courier.getFinishedCnt());
        return view;
    }
}
