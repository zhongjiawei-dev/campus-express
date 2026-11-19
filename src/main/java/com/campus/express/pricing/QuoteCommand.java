package com.campus.express.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuoteCommand(
        Long stationId,
        int orderType,
        int weightGram,
        Long addressId,
        BigDecimal lat,
        BigDecimal lng,
        LocalDateTime expectTime,
        boolean insured,
        BigDecimal declaredValue,
        BigDecimal discount
) {
}
