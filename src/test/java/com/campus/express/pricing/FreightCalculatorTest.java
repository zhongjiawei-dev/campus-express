package com.campus.express.pricing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FreightCalculatorTest {

    @Test
    void ordinaryPackageShouldBe380() {
        PriceResult result = FreightCalculator.calculate(rule(1),
                2000, 700, LocalDateTime.of(2026, 11, 19, 10, 0),
                false, null, BigDecimal.ZERO);
        assertEquals(new BigDecimal("3.80"), result.totalFee());
        assertEquals(new BigDecimal("3.00"), result.baseFee());
        assertEquals(new BigDecimal("0.80"), result.distanceFee());
        assertFalse(result.peak());
    }

    @Test
    void bigPackageAtPeakShouldBe1520() {
        PriceResult result = FreightCalculator.calculate(rule(2),
                8000, 1200, LocalDateTime.of(2026, 11, 19, 12, 0),
                false, null, BigDecimal.ZERO);
        assertEquals(new BigDecimal("15.20"), result.totalFee());
        assertEquals(new BigDecimal("5.00"), result.weightFee());
        assertEquals(new BigDecimal("1.60"), result.distanceFee());
        assertEquals(new BigDecimal("0.60"), result.peakFee());
        assertTrue(result.peak());
    }

    @Test
    void freshPackageShouldBe1080() {
        PriceResult result = FreightCalculator.calculate(rule(3),
                7000, 1000, LocalDateTime.of(2026, 11, 19, 15, 0),
                false, null, BigDecimal.ZERO);
        assertEquals(new BigDecimal("10.80"), result.totalFee());
        assertEquals(new BigDecimal("3.00"), result.typeExtraFee());
    }

    @Test
    void weightStepShouldRoundUp() {
        PriceResult result = FreightCalculator.calculate(rule(1),
                3001, 500, LocalDateTime.of(2026, 11, 19, 10, 0),
                false, null, BigDecimal.ZERO);
        assertEquals(new BigDecimal("4.00"), result.totalFee());
    }

    @Test
    void peakWindowBoundaryShouldBeInclusive() {
        assertTrue(FreightCalculator.inPeak("11:30-13:00,17:00-18:30",
                LocalDateTime.of(2026, 11, 19, 13, 0)));
        assertFalse(FreightCalculator.inPeak("11:30-13:00,17:00-18:30",
                LocalDateTime.of(2026, 11, 19, 13, 1)));
    }

    @Test
    void insureFeeShouldHaveFloorAndCap() {
        PriceResult low = FreightCalculator.calculate(rule(1), 1000, 300,
                LocalDateTime.of(2026, 11, 19, 10, 0), true, new BigDecimal("10"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.50"), low.insureFee());

        PriceResult high = FreightCalculator.calculate(rule(1), 1000, 300,
                LocalDateTime.of(2026, 11, 19, 10, 0), true, new BigDecimal("5000"), BigDecimal.ZERO);
        assertEquals(new BigDecimal("16.00"), high.insureFee());
    }

    @Test
    void totalShouldRespectMinAndMax() {
        PriceResult low = FreightCalculator.calculate(rule(1), 500, 200,
                LocalDateTime.of(2026, 11, 19, 10, 0), false, null, new BigDecimal("10"));
        assertEquals(new BigDecimal("2.00"), low.totalFee());

        PriceResult high = FreightCalculator.calculate(rule(1), 30000, 20000,
                LocalDateTime.of(2026, 11, 19, 10, 0), false, null, BigDecimal.ZERO);
        assertEquals(new BigDecimal("50.00"), high.totalFee());
    }

    private PriceRule rule(int type) {
        PriceRule rule = new PriceRule();
        rule.setRuleNo("TEST-T" + type);
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
        return rule;
    }
}
