package com.campus.express.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 运费计算：基础配送费 + 重量加价 + 距离加价 + 高峰溢价 + 品类附加 + 保价费 - 优惠。
 * 纯函数实现，便于单元测试与规则回归。
 */
public final class FreightCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX_DECLARED = new BigDecimal("2000");

    private FreightCalculator() {
    }

    public static PriceResult calculate(PriceRule rule,
                                        int weightGram,
                                        int distanceM,
                                        LocalDateTime expectTime,
                                        boolean insured,
                                        BigDecimal declaredValue,
                                        BigDecimal discount) {
        BigDecimal base = nz(rule.getBaseFee(), "3.00");

        int overWeight = Math.max(0, weightGram - nz(rule.getFreeWeightGram(), 3000));
        int weightSteps = ceilDiv(overWeight, Math.max(1, nz(rule.getWeightStepGram(), 1000)));
        BigDecimal weightFee = nz(rule.getWeightStepFee(), "1.00")
                .multiply(BigDecimal.valueOf(weightSteps));

        int overDistance = Math.max(0, distanceM - nz(rule.getFreeDistanceM(), 500));
        int distanceSteps = ceilDiv(overDistance, Math.max(1, nz(rule.getDistanceStepM(), 500)));
        BigDecimal distanceFee = nz(rule.getDistanceStepFee(), "0.80")
                .multiply(BigDecimal.valueOf(distanceSteps));

        boolean peak = inPeak(rule.getPeakWindows(), expectTime);
        BigDecimal peakFee = peak
                ? base.multiply(nz(rule.getPeakRate(), "20.00")).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal typeExtra = nz(rule.getTypeExtraFee(), "0.00");

        BigDecimal insureFee = BigDecimal.ZERO;
        if (insured && declaredValue != null && declaredValue.signum() > 0) {
            BigDecimal capped = declaredValue.min(MAX_DECLARED);
            insureFee = capped.multiply(nz(rule.getInsureRate(), "0.0080"))
                    .max(nz(rule.getInsureMinFee(), "0.50"));
        }

        BigDecimal discountAmount = discount == null ? BigDecimal.ZERO : discount.max(BigDecimal.ZERO);

        BigDecimal sum = base.add(weightFee).add(distanceFee).add(peakFee)
                .add(typeExtra).add(insureFee).subtract(discountAmount);
        BigDecimal total = sum.max(nz(rule.getMinFee(), "2.00"))
                .min(nz(rule.getMaxFee(), "50.00"))
                .setScale(2, RoundingMode.HALF_UP);

        return new PriceResult(
                scale(base), scale(weightFee), scale(distanceFee), scale(peakFee),
                scale(typeExtra), scale(insureFee), scale(discountAmount), total,
                distanceM, weightGram, peak, rule.getRuleNo());
    }

    /** 判断预约时间是否落在高峰时段，如 "11:30-13:00,17:00-18:30" */
    public static boolean inPeak(String windows, LocalDateTime time) {
        if (windows == null || windows.isBlank() || time == null) {
            return false;
        }
        LocalTime current = time.toLocalTime();
        for (String window : windows.split(",")) {
            String[] parts = window.trim().split("-");
            if (parts.length != 2) {
                continue;
            }
            LocalTime start = parseTime(parts[0]);
            LocalTime end = parseTime(parts[1]);
            if (start == null || end == null) {
                continue;
            }
            if (!current.isBefore(start) && !current.isAfter(end)) {
                return true;
            }
        }
        return false;
    }

    private static LocalTime parseTime(String text) {
        try {
            String[] hm = text.trim().split(":");
            return LocalTime.of(Integer.parseInt(hm[0]), Integer.parseInt(hm[1]));
        } catch (Exception e) {
            return null;
        }
    }

    private static int ceilDiv(int value, int step) {
        return value <= 0 ? 0 : (value + step - 1) / step;
    }

    private static BigDecimal nz(BigDecimal value, String fallback) {
        return value == null ? new BigDecimal(fallback) : value;
    }

    private static int nz(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
