package com.campus.express.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.AesUtil;
import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;
import com.campus.express.common.LoginUser;
import com.campus.express.common.MaskUtil;
import com.campus.express.common.SensitiveWordFilter;
import com.campus.express.complaint.Complaint;
import com.campus.express.complaint.mapper.ComplaintMapper;
import com.campus.express.courier.Courier;
import com.campus.express.courier.CourierService;
import com.campus.express.message.MessageService;
import com.campus.express.order.mapper.OrderMapper;
import com.campus.express.order.mapper.OrderStatusLogMapper;
import com.campus.express.pricing.PriceResult;
import com.campus.express.pricing.PricingService;
import com.campus.express.pricing.QuoteCommand;
import com.campus.express.station.Station;
import com.campus.express.station.mapper.StationMapper;
import com.campus.express.user.Address;
import com.campus.express.user.mapper.AddressMapper;
import com.campus.express.wallet.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal COURIER_SHARE = new BigDecimal("0.90");
    private static final BigDecimal CANCEL_PENALTY = new BigDecimal("0.20");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderMapper orderMapper;
    private final OrderStatusLogMapper logMapper;
    private final AddressMapper addressMapper;
    private final StationMapper stationMapper;
    private final PricingService pricingService;
    private final OrderStateService stateService;
    private final WalletService walletService;
    private final CourierService courierService;
    private final MessageService messageService;
    private final AesUtil aesUtil;
    private final ObjectMapper objectMapper;
    private final ComplaintMapper complaintMapper;
    private final SensitiveWordFilter sensitiveWordFilter;

    // ---------------------------------------------------------------- 下单

    @Transactional
    public Map<String, Object> create(Long userId, CreateRequest request) {
        if (request.orderType() == null || request.orderType() < 1 || request.orderType() > 3) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择快递类型");
        }
        if (request.weightGram() == null || request.weightGram() <= 0 || request.weightGram() > 30000) {
            throw new BizException(ErrorCode.PARAM_ERROR, "重量需在 0-30kg 之间");
        }
        if (request.pickupCode() == null || !request.pickupCode().matches("\\d{4,8}")) {
            throw new BizException(ErrorCode.PARAM_ERROR, "取件码应为 4-8 位数字");
        }
        Address address = addressMapper.selectById(request.addressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择有效的收货地址");
        }
        Station station = stationMapper.selectById(request.stationId());
        if (station == null || station.getStatus() == null || station.getStatus() != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "快递站点不存在或已停用");
        }

        LocalDateTime expectStart = request.expectStart() == null
                ? LocalDateTime.now().plusHours(1) : request.expectStart();
        LocalDateTime expectEnd = request.expectEnd() == null
                ? expectStart.plusHours(2) : request.expectEnd();
        if (expectEnd.isBefore(expectStart)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "预约时间区间不合法");
        }
        if (expectStart.isBefore(LocalDateTime.now().minusMinutes(10))) {
            throw new BizException(ErrorCode.PARAM_ERROR, "预约时间不能早于当前时间");
        }

        PriceResult price = pricingService.quote(userId, new QuoteCommand(
                request.stationId(), request.orderType(), request.weightGram(), request.addressId(),
                address.getLat(), address.getLng(), expectStart,
                Boolean.TRUE.equals(request.insured()), request.declaredValue(), request.discount()));

        Order order = new Order();
        order.setOrderNo(genOrderNo(station.getId()));
        order.setUserId(userId);
        order.setStationId(station.getId());
        order.setOrderType(request.orderType());
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setWeightGram(request.weightGram());
        order.setExpectStart(expectStart);
        order.setExpectEnd(expectEnd);
        order.setAddressId(address.getId());
        order.setAddressSnapshot(aesUtil.encrypt(toJson(snapshotAddress(address))));
        order.setDistanceM(price.distanceM());
        order.setBaseFee(price.baseFee());
        order.setWeightFee(price.weightFee());
        order.setDistanceFee(price.distanceFee());
        order.setPeakFee(price.peakFee());
        order.setTypeExtraFee(price.typeExtraFee());
        order.setInsureFee(price.insureFee());
        order.setDiscountAmount(price.discountAmount());
        order.setTotalFee(price.totalFee());
        order.setPayFee(BigDecimal.ZERO);
        order.setPriceSnapshot(toJson(price));
        order.setPickupCode(aesUtil.encrypt(request.pickupCode()));
        order.setPickupCodeMask(MaskUtil.tail(request.pickupCode(), 2));
        order.setPickupCodeVerified(0);
        order.setInsureFlag(Boolean.TRUE.equals(request.insured()) ? 1 : 0);
        order.setDeclaredValue(request.declaredValue());
        order.setPayType(request.payType() == null ? 1 : request.payType());
        order.setRemark(sanitize(request.remark()));
        order.setUrgent(Boolean.TRUE.equals(request.urgent()) ? 1 : 0);
        order.setVersion(0);
        orderMapper.insert(order);

        messageService.send(userId, "下单成功",
                "订单 " + order.getOrderNo() + " 待支付 ¥" + price.totalFee() + "，请在 15 分钟内完成支付。",
                order.getOrderNo());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("totalFee", price.totalFee());
        result.put("distanceM", price.distanceM());
        result.put("peak", price.peak());
        result.put("price", price);
        return result;
    }

    public PriceResult quote(Long userId, QuoteCommand command) {
        return pricingService.quote(userId, command);
    }

    // ---------------------------------------------------------------- 支付

    @Transactional
    public Order pay(String orderNo, Long userId) {
        Order order = stateService.findByNo(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAY.getCode()) {
            throw new BizException(ErrorCode.ORDER_STATE_ILLEGAL, "订单当前不可支付");
        }
        if (order.getPayTime() != null) {
            throw new BizException(ErrorCode.PAY_DUPLICATED);
        }
        int payType = order.getPayType() == null ? 1 : order.getPayType();
        if (payType == 1) {
            walletService.debit(userId, order.getTotalFee(), 2, orderNo, "订单支付");
        }
        String payNo = "MOCK" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000);
        orderMapper.updatePayInfo(orderNo, payNo, payType);
        orderMapper.updatePayFee(orderNo, order.getTotalFee());
        return stateService.fire(orderNo, OrderEvent.PAY, Operator.user(userId), "支付成功");
    }

    // ---------------------------------------------------------------- 取消 / 退款

    @Transactional
    public Order cancel(String orderNo, Long userId, String reason, boolean byAdmin) {
        Order order = stateService.findByNo(orderNo);
        Operator operator = byAdmin ? Operator.admin(userId) : Operator.user(userId);
        int status = order.getStatus();

        if (status == OrderStatus.PENDING_PAY.getCode() || status == OrderStatus.WAIT_ACCEPT.getCode()) {
            orderMapper.updateCancelInfo(orderNo, byAdmin ? 3 : 1, sanitize(reason));
            Order cancelled = stateService.fire(orderNo, OrderEvent.CANCEL_BY_USER, operator, reason);
            if (order.getPayTime() != null) {
                refund(order, order.getTotalFee(), "取消订单全额退款");
            }
            return cancelled;
        }

        if (status == OrderStatus.ACCEPTED.getCode()) {
            orderMapper.updateCancelInfo(orderNo, 1, sanitize(reason));
            stateService.fire(orderNo, OrderEvent.CANCEL, operator, reason);
            BigDecimal penalty = order.getTotalFee().multiply(CANCEL_PENALTY).setScale(2, RoundingMode.HALF_UP);
            BigDecimal refundAmount = order.getTotalFee().subtract(penalty);
            refund(order, refundAmount, "接单后取消，扣除 20% 违约金");
            if (order.getCourierId() != null) {
                Courier courier = courierService.byId(order.getCourierId());
                if (courier != null) {
                    walletService.credit(courier.getUserId(), penalty, 4, orderNo, "订单取消补偿");
                }
            }
            return stateService.fire(orderNo, OrderEvent.REFUND_SUCCESS, operator, "退款完成");
        }

        throw new BizException(ErrorCode.ORDER_STATE_ILLEGAL, "当前状态不支持取消，请发起售后");
    }

    private void refund(Order order, BigDecimal amount, String remark) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        walletService.credit(order.getUserId(), amount, 4, order.getOrderNo(), remark);
    }

    // ---------------------------------------------------------------- 履约

    @Transactional
    public Order pick(String orderNo, Long userId, String pickupCode, String photoUrl) {
        Courier courier = courierService.requireApproved(userId);
        Order order = stateService.findByNo(orderNo);
        if (!courier.getId().equals(order.getCourierId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能操作自己接的订单");
        }
        if (order.getStatus() != OrderStatus.ACCEPTED.getCode()) {
            throw new BizException(ErrorCode.ORDER_STATE_ILLEGAL, "订单当前不可取件");
        }
        String realCode = aesUtil.decrypt(order.getPickupCode());
        if (pickupCode == null || !realCode.equals(pickupCode.trim())) {
            throw new BizException(ErrorCode.PICKUP_CODE_ERROR);
        }
        orderMapper.markPickupVerified(orderNo);
        if (photoUrl != null && !photoUrl.isBlank()) {
            orderMapper.updatePickPhoto(orderNo, photoUrl);
        }
        return stateService.fire(orderNo, OrderEvent.PICK, Operator.courier(userId, courier.getId()),
                "取件码核验通过");
    }

    @Transactional
    public Order deliver(String orderNo, Long userId, String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请上传送达凭证");
        }
        Courier courier = courierService.requireApproved(userId);
        Order order = stateService.findByNo(orderNo);
        if (!courier.getId().equals(order.getCourierId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能操作自己接的订单");
        }
        orderMapper.updateDeliverPhoto(orderNo, photoUrl);
        return stateService.fire(orderNo, OrderEvent.DELIVER, Operator.courier(userId, courier.getId()),
                "已上传送达凭证");
    }

    @Transactional
    public Order confirm(String orderNo, Long userId) {
        Order order = stateService.findByNo(orderNo);
        Order completed = stateService.fire(orderNo, OrderEvent.CONFIRM, Operator.user(userId), "用户确认收货");
        if (order.getCourierId() != null) {
            Courier courier = courierService.byId(order.getCourierId());
            if (courier != null) {
                BigDecimal income = order.getTotalFee().multiply(COURIER_SHARE).setScale(2, RoundingMode.HALF_UP);
                walletService.credit(courier.getUserId(), income, 3, orderNo, "订单结算收入（平台抽成 10%）");
                courierService.increaseFinished(courier.getId());
            }
        }
        return completed;
    }

    // ---------------------------------------------------------------- 查询

    public Map<String, Object> detail(String orderNo, Long userId, String role) {
        Order order = stateService.findByNo(orderNo);
        boolean isOwner = order.getUserId().equals(userId);
        boolean isAdmin = LoginUser.ADMIN.equals(role);
        boolean isCourier = false;
        if (LoginUser.COURIER.equals(role) && order.getCourierId() != null) {
            Courier courier = courierService.findByUser(userId);
            isCourier = courier != null && courier.getId().equals(order.getCourierId());
        }
        if (!isOwner && !isAdmin && !isCourier) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权查看该订单");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", orderView(order));
        result.put("address", readAddress(order));
        result.put("statusText", OrderStatus.descOf(order.getStatus()));
        result.put("logs", logMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderNo, orderNo)
                .orderByAsc(OrderStatusLog::getCreatedAt)));
        Station station = stationMapper.selectById(order.getStationId());
        result.put("stationName", station == null ? "" : station.getName());
        if (order.getCourierId() != null) {
            Courier courier = courierService.byId(order.getCourierId());
            if (courier != null) {
                result.put("courierName", MaskUtil.name(courier.getRealName()));
            }
        }
        return result;
    }

    public IPage<Map<String, Object>> listMine(Long userId, String group, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId);
        applyGroup(wrapper, group);
        wrapper.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectPage(new Page<>(page, size), wrapper).convert(this::orderView);
    }

    public IPage<Map<String, Object>> listCourier(Long userId, String group, int page, int size) {
        Courier courier = courierService.requireApproved(userId);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getCourierId, courier.getId());
        applyGroup(wrapper, group);
        wrapper.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectPage(new Page<>(page, size), wrapper).convert(this::orderView);
    }

    public IPage<Map<String, Object>> hall(String sort, Long stationId, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.WAIT_ACCEPT.getCode());
        if (stationId != null) {
            wrapper.eq(Order::getStationId, stationId);
        }
        switch (sort == null ? "" : sort) {
            case "price_desc" -> wrapper.orderByDesc(Order::getTotalFee);
            case "price_asc" -> wrapper.orderByAsc(Order::getTotalFee);
            case "distance_asc" -> wrapper.orderByAsc(Order::getDistanceM);
            default -> wrapper.orderByDesc(Order::getCreatedAt);
        }
        return orderMapper.selectPage(new Page<>(page, size), wrapper).convert(order -> {
            Map<String, Object> view = orderView(order);
            Station station = stationMapper.selectById(order.getStationId());
            view.put("stationName", station == null ? "" : station.getName());
            return view;
        });
    }

    // ---------------------------------------------------------------- 售后

    @Transactional
    public Complaint appeal(String orderNo, Long userId, int type, String description, String demand, String images) {
        Order order = stateService.findByNo(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        stateService.fire(orderNo, OrderEvent.AFTER_SALE, Operator.user(userId), "用户发起售后");

        Complaint complaint = new Complaint();
        complaint.setComplaintNo("CP" + System.currentTimeMillis()
                + ThreadLocalRandom.current().nextInt(1000, 9999));
        complaint.setOrderNo(orderNo);
        complaint.setUserId(userId);
        complaint.setCourierId(order.getCourierId());
        complaint.setType(type);
        complaint.setDescription(sensitiveWordFilter.sanitize(description));
        complaint.setDemand(demand);
        complaint.setImages(images);
        complaint.setStatus(0);
        complaint.setDeadline(LocalDateTime.now().plusHours(24));
        complaintMapper.insert(complaint);
        messageService.send(userId, "售后已受理", "平台将在 24 小时内联系你处理订单 " + orderNo + " 的售后。", orderNo);
        return complaint;
    }

    /**
     * 仲裁结束后把订单从「售后处理中」恢复为「已完成」，避免订单永久停留在 100。
     */
    @Transactional
    public void closeAfterSale(String orderNo, Long adminId) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        Order order = stateService.findByNo(orderNo);
        if (order.getStatus() == OrderStatus.AFTER_SALE.getCode()) {
            stateService.fire(orderNo, OrderEvent.RESOLVE, Operator.admin(adminId), "平台仲裁完成");
        }
    }

    // ---------------------------------------------------------------- 内部方法

    private void applyGroup(LambdaQueryWrapper<Order> wrapper, String group) {
        if (group == null || group.isBlank() || "all".equals(group)) {
            return;
        }
        switch (group) {
            case "active" -> wrapper.in(Order::getStatus,
                    OrderStatus.PENDING_PAY.getCode(), OrderStatus.WAIT_ACCEPT.getCode(),
                    OrderStatus.ACCEPTED.getCode(), OrderStatus.PICKED.getCode(),
                    OrderStatus.DELIVERED.getCode());
            case "done" -> wrapper.in(Order::getStatus, OrderStatus.COMPLETED.getCode(),
                    OrderStatus.REFUNDED.getCode());
            case "refund" -> wrapper.in(Order::getStatus, OrderStatus.CANCELLED.getCode(),
                    OrderStatus.REFUNDING.getCode(), OrderStatus.REFUNDED.getCode());
            default -> {
            }
        }
    }

    private Map<String, Object> orderView(Order order) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("orderNo", order.getOrderNo());
        view.put("status", order.getStatus());
        view.put("statusText", OrderStatus.descOf(order.getStatus()));
        view.put("orderType", order.getOrderType());
        view.put("typeText", typeText(order.getOrderType()));
        view.put("stationId", order.getStationId());
        view.put("addressId", order.getAddressId());
        view.put("weightGram", order.getWeightGram());
        view.put("distanceM", order.getDistanceM());
        view.put("totalFee", order.getTotalFee());
        view.put("payFee", order.getPayFee());
        view.put("pickupCodeMask", order.getPickupCodeMask());
        view.put("expectStart", order.getExpectStart());
        view.put("expectEnd", order.getExpectEnd());
        view.put("remark", order.getRemark());
        view.put("insureFlag", order.getInsureFlag());
        view.put("declaredValue", order.getDeclaredValue());
        view.put("urgent", order.getUrgent());
        view.put("createdAt", order.getCreatedAt());
        view.put("acceptTime", order.getAcceptTime());
        view.put("pickTime", order.getPickTime());
        view.put("deliverTime", order.getDeliverTime());
        view.put("finishTime", order.getFinishTime());
        view.put("photoDeliverUrl", order.getPhotoDeliverUrl());
        view.put("payType", order.getPayType());
        return view;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readAddress(Order order) {
        if (order.getAddressSnapshot() == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(aesUtil.decrypt(order.getAddressSnapshot()), Map.class);
        } catch (Exception e) {
            log.warn("read address snapshot failed orderNo={}", order.getOrderNo());
            throw new BizException(ErrorCode.SYSTEM_ERROR, "地址信息解析失败");
        }
    }

    private Map<String, Object> snapshotAddress(Address address) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("contactName", address.getContactName());
        snapshot.put("phoneMask", address.getPhoneMask());
        snapshot.put("campus", address.getCampus());
        snapshot.put("building", address.getBuilding());
        snapshot.put("room", address.getRoomMask());
        snapshot.put("detail", safeDetail(address.getDetail()));
        snapshot.put("lat", address.getLat());
        snapshot.put("lng", address.getLng());
        return snapshot;
    }

    /**
     * 地址明细在库内为密文，写入订单快照前先解密，避免二次加密。
     */
    private String safeDetail(String value) {
        if (value == null) {
            return null;
        }
        try {
            return aesUtil.decrypt(value);
        } catch (Exception e) {
            return value;
        }
    }

    private String typeText(Integer type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case 1 -> "普通件";
            case 2 -> "大件";
            case 3 -> "生鲜冷链";
            default -> "其他";
        };
    }

    private String sanitize(String text) {
        String value = sensitiveWordFilter.sanitize(text);
        return value == null ? null : value.trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "数据序列化失败");
        }
    }

    private String genOrderNo(Long stationId) {
        return "CE" + LocalDate.now().format(DAY)
                + String.format("%02d", stationId % 100)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    public record CreateRequest(
            Long stationId,
            Integer orderType,
            Integer weightGram,
            String pickupCode,
            Long addressId,
            LocalDateTime expectStart,
            LocalDateTime expectEnd,
            String remark,
            Boolean insured,
            BigDecimal declaredValue,
            BigDecimal discount,
            Integer payType,
            Boolean urgent
    ) {
    }
}
