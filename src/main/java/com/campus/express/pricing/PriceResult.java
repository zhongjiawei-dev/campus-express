package com.campus.express.pricing;

import java.math.BigDecimal;

public record PriceResult(
        BigDecimal baseFee,
        BigDecimal weightFee,
        BigDecimal distanceFee,
        BigDecimal peakFee,
        BigDecimal typeExtraFee,
        BigDecimal insureFee,
        BigDecimal discountAmount,
        BigDecimal totalFee,
        int distanceM,
        int weightGram,
        boolean peak,
        String ruleNo
) {
}
