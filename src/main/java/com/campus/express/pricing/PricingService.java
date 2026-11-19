package com.campus.express.pricing;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;
import com.campus.express.pricing.mapper.PriceRuleMapper;
import com.campus.express.station.Station;
import com.campus.express.station.mapper.StationMapper;
import com.campus.express.user.Address;
import com.campus.express.user.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final PriceRuleMapper ruleMapper;
    private final StationMapper stationMapper;
    private final AddressMapper addressMapper;
    private final DistanceService distanceService;

    public PriceResult quote(Long userId, QuoteCommand command) {
        Station station = stationMapper.selectById(command.stationId());
        if (station == null || station.getStatus() == null || station.getStatus() != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "快递站点不存在或已停用");
        }

        BigDecimal lat;
        BigDecimal lng;
        if (command.addressId() != null) {
            Address address = addressMapper.selectById(command.addressId());
            if (address == null || !address.getUserId().equals(userId)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "收货地址不存在");
            }
            lat = address.getLat();
            lng = address.getLng();
        } else {
            lat = command.lat();
            lng = command.lng();
        }

        int meters = distanceService.meters(station.getLat(), station.getLng(), lat, lng);
        PriceRule rule = ruleMapper.matchStation(command.stationId(), command.orderType());
        if (rule == null) {
            rule = ruleMapper.matchGlobal(command.orderType());
        }
        if (rule == null) {
            rule = defaultRule(command.orderType());
        }
        return FreightCalculator.calculate(rule, command.weightGram(), meters, command.expectTime(),
                command.insured(), command.declaredValue(), command.discount());
    }

    /** 数据库无规则时的兜底配置，与方案默认口径一致 */
    public PriceRule defaultRule(int orderType) {
        PriceRule rule = new PriceRule();
        rule.setRuleNo("DEFAULT-" + orderType);
        rule.setOrderType(orderType);
        rule.setBaseFee(new BigDecimal("3.00"));
        rule.setFreeWeightGram(3000);
        rule.setWeightStepGram(1000);
        rule.setWeightStepFee(new BigDecimal("1.00"));
        rule.setFreeDistanceM(500);
        rule.setDistanceStepM(500);
        rule.setDistanceStepFee(new BigDecimal("0.80"));
        rule.setPeakRate(new BigDecimal("20.00"));
        rule.setPeakWindows("11:30-13:00,17:00-18:30");
        rule.setTypeExtraFee(orderType == 2 ? new BigDecimal("5.00")
                : orderType == 3 ? new BigDecimal("3.00") : BigDecimal.ZERO);
        rule.setInsureRate(new BigDecimal("0.0080"));
        rule.setInsureMinFee(new BigDecimal("0.50"));
        rule.setMinFee(new BigDecimal("2.00"));
        rule.setMaxFee(new BigDecimal("50.00"));
        return rule;
    }
}
