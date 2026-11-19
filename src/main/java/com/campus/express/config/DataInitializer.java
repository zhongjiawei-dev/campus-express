package com.campus.express.config;

import com.campus.express.admin.Admin;
import com.campus.express.admin.mapper.AdminMapper;
import com.campus.express.common.AesUtil;
import com.campus.express.courier.Courier;
import com.campus.express.courier.mapper.CourierMapper;
import com.campus.express.order.Order;
import com.campus.express.order.OrderStatus;
import com.campus.express.order.mapper.OrderMapper;
import com.campus.express.pricing.PriceRule;
import com.campus.express.pricing.mapper.PriceRuleMapper;
import com.campus.express.station.Station;
import com.campus.express.station.mapper.StationMapper;
import com.campus.express.user.Address;
import com.campus.express.user.User;
import com.campus.express.user.mapper.AddressMapper;
import com.campus.express.user.mapper.UserMapper;
import com.campus.express.wallet.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final StationMapper stationMapper;
    private final CourierMapper courierMapper;
    private final AdminMapper adminMapper;
    private final PriceRuleMapper priceRuleMapper;
    private final OrderMapper orderMapper;
    private final WalletService walletService;
    private final AesUtil aesUtil;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        seedStations();
        User student = seedUser("wx_student", "林同学", "138****8000", "202303704");
        User courierUser = seedUser("wx_courier", "周学长", "139****1234", "202201234");

        Address address = seedAddress(student.getId());
        seedCourier(courierUser.getId());
        seedPriceRules();
        seedAdmin();
        walletService.credit(student.getId(), new BigDecimal("100.00"), 1, "INIT", "演示账户充值");

        seedOrder(student.getId(), address.getId(), "8821", OrderStatus.WAIT_ACCEPT.getCode(),
                new BigDecimal("3.80"), null, LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2));
        seedOrder(student.getId(), address.getId(), "5520", OrderStatus.COMPLETED.getCode(),
                new BigDecimal("6.60"), 1L, LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1).plusHours(2));

        log.info("==============================================================");
        log.info("演示数据初始化完成");
        log.info("学生账号  code=student   角色 USER");
        log.info("代取员    code=courier   角色 COURIER");
        log.info("管理员    admin / admin123");
        log.info("订单取件码：8821（待接单演示单）");
        log.info("==============================================================");
    }

    private void seedStations() {
        createStation("菜鸟驿站·东区店", 1, "东校区", "东校区生活区 1 号门内", "30.2760", "120.1540", "08:00", "21:00", 1);
        createStation("丰巢快递柜·图书馆", 2, "中心校区", "图书馆一层北侧", "30.2730", "120.1570", "00:00", "24:00", 2);
        createStation("京东快递·南门站", 2, "南校区", "南门快递集散点", "30.2705", "120.1520", "08:30", "20:00", 3);
        createStation("顺丰驿站·西区", 1, "西校区", "西区 3 号宿舍楼下", "30.2775", "120.1505", "08:00", "20:30", 4);
    }

    private void createStation(String name, int brand, String campus, String address,
                               String lat, String lng, String open, String close, int sort) {
        Station station = new Station();
        station.setName(name);
        station.setBrand(brand);
        station.setCampus(campus);
        station.setAddress(address);
        station.setLat(new BigDecimal(lat));
        station.setLng(new BigDecimal(lng));
        station.setOpenTime(open);
        station.setCloseTime(close);
        station.setStatus(1);
        station.setSort(sort);
        stationMapper.insert(station);
    }

    private User seedUser(String openId, String nickname, String phoneMask, String studentNo) {
        User user = new User();
        user.setOpenId(openId);
        user.setNickname(nickname);
        user.setPhoneMask(phoneMask);
        user.setStudentNo(studentNo);
        user.setStatus(0);
        user.setCreditScore(100);
        user.setInviteCode("CAMPUS");
        userMapper.insert(user);
        walletService.getOrCreate(user.getId());
        return user;
    }

    private Address seedAddress(Long userId) {
        Address address = new Address();
        address.setUserId(userId);
        address.setContactName("林同学");
        address.setPhoneMask("138****8000");
        address.setCampus("东校区");
        address.setBuilding("6 号楼");
        address.setRoomMask("512");
        address.setDetail(aesUtil.encrypt("6 号楼 512 室，门口有蓝色地垫"));
        address.setLat(new BigDecimal("30.2752"));
        address.setLng(new BigDecimal("120.1532"));
        address.setIsDefault(1);
        address.setStatus(0);
        addressMapper.insert(address);
        return address;
    }

    private void seedCourier(Long userId) {
        Courier courier = new Courier();
        courier.setUserId(userId);
        courier.setRealName("周明");
        courier.setStudentNo("202201234");
        courier.setIdCardMask("3302**********1234");
        courier.setStudentCardUrl("https://example.com/student-card-demo.jpg");
        courier.setAuditStatus(Courier.AUDIT_APPROVED);
        courier.setAuditRemark("演示数据，已审核");
        courier.setWorkStatus(1);
        courier.setServiceStart("08:00");
        courier.setServiceEnd("21:00");
        courier.setMaxWeightGram(8000);
        courier.setStationIds("1,2");
        courier.setLevel(2);
        courier.setScore(new BigDecimal("4.90"));
        courier.setFinishedCnt(37);
        courier.setViolationCnt(0);
        courierMapper.insert(courier);
    }

    private void seedPriceRules() {
        for (int type = 1; type <= 3; type++) {
            PriceRule rule = new PriceRule();
            rule.setRuleNo("GLOBAL-T" + type);
            rule.setOrderType(type);
            rule.setBaseFee(new BigDecimal("3.00"));
            rule.setFreeWeightGram(3000);
            rule.setWeightStepGram(1000);
            rule.setWeightStepFee(new BigDecimal("1.00"));
            rule.setFreeDistanceM(500);
            rule.setDistanceStepM(500);
            rule.setDistanceStepFee(new BigDecimal("0.80"));
            rule.setPeakRate(new BigDecimal("20.00"));
            rule.setPeakWindows("11:30-13:00,17:00-18:30");
            rule.setTypeExtraFee(type == 2 ? new BigDecimal("5.00")
                    : type == 3 ? new BigDecimal("3.00") : BigDecimal.ZERO);
            rule.setInsureRate(new BigDecimal("0.0080"));
            rule.setInsureMinFee(new BigDecimal("0.50"));
            rule.setMinFee(new BigDecimal("2.00"));
            rule.setMaxFee(new BigDecimal("50.00"));
            rule.setStatus(1);
            rule.setVersion(1);
            priceRuleMapper.insert(rule);
        }
    }

    private void seedAdmin() {
        Admin admin = new Admin();
        admin.setUsername("admin");
        admin.setPasswordHash(new BCryptPasswordEncoder().encode("admin123"));
        admin.setRealName("平台管理员");
        admin.setRoleCode("super");
        admin.setStatus(1);
        adminMapper.insert(admin);
    }

    private void seedOrder(Long userId, Long addressId, String pickupCode, int status,
                           BigDecimal totalFee, Long courierId, LocalDateTime start, LocalDateTime end)
            throws Exception {
        Order order = new Order();
        order.setOrderNo("CE2026111900000" + (status == 20 ? "01" : "02"));
        order.setUserId(userId);
        order.setStationId(1L);
        order.setCourierId(courierId);
        order.setOrderType(1);
        order.setStatus(status);
        order.setWeightGram(2000);
        order.setPickupCode(aesUtil.encrypt(pickupCode));
        order.setPickupCodeMask("**" + pickupCode.substring(pickupCode.length() - 2));
        order.setPickupCodeVerified(status >= 40 ? 1 : 0);
        order.setExpectStart(start);
        order.setExpectEnd(end);
        order.setAddressId(addressId);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("contactName", "林同学");
        snapshot.put("phoneMask", "138****8000");
        snapshot.put("campus", "东校区");
        snapshot.put("building", "6 号楼");
        snapshot.put("room", "512");
        snapshot.put("detail", "6 号楼 512 室，门口有蓝色地垫");
        snapshot.put("lat", 30.2752);
        snapshot.put("lng", 120.1532);
        order.setAddressSnapshot(aesUtil.encrypt(objectMapper.writeValueAsString(snapshot)));
        order.setDistanceM(620);
        order.setBaseFee(new BigDecimal("3.00"));
        order.setWeightFee(BigDecimal.ZERO);
        order.setDistanceFee(totalFee.subtract(new BigDecimal("3.00")));
        order.setPeakFee(BigDecimal.ZERO);
        order.setTypeExtraFee(BigDecimal.ZERO);
        order.setInsureFee(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalFee(totalFee);
        order.setPayFee(totalFee);
        order.setPayType(1);
        order.setPayNo("MOCK-DEMO-" + status);
        order.setPayTime(LocalDateTime.now().minusHours(2));
        if (status >= 30) {
            order.setAcceptTime(LocalDateTime.now().minusHours(1));
        }
        if (status >= 40) {
            order.setPickTime(LocalDateTime.now().minusMinutes(40));
        }
        if (status >= 50) {
            order.setDeliverTime(LocalDateTime.now().minusMinutes(10));
        }
        if (status == 60) {
            order.setFinishTime(LocalDateTime.now());
            order.setPhotoDeliverUrl("https://example.com/proof-demo.jpg");
        }
        order.setInsureFlag(0);
        order.setUrgent(0);
        order.setVersion(0);
        order.setPriceSnapshot("{\"ruleNo\":\"GLOBAL-T1\",\"totalFee\":" + totalFee + "}");
        orderMapper.insert(order);
    }
}
