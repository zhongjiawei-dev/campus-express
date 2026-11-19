package com.campus.express.pricing;

import com.campus.express.common.Result;
import com.campus.express.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/price")
@RequiredArgsConstructor
public class PriceController {

    private final PricingService pricingService;

    @PostMapping("/quote")
    public Result<PriceResult> quote(@RequestBody QuoteRequest request) {
        return Result.ok(pricingService.quote(UserContext.userId(), new QuoteCommand(
                request.stationId(),
                request.orderType() == null ? 1 : request.orderType(),
                request.weightGram() == null ? 1000 : request.weightGram(),
                request.addressId(), request.lat(), request.lng(),
                request.expectTime() == null ? LocalDateTime.now() : request.expectTime(),
                Boolean.TRUE.equals(request.insured()), request.declaredValue(), request.discount())));
    }

    public record QuoteRequest(
            Long stationId,
            Integer orderType,
            Integer weightGram,
            Long addressId,
            BigDecimal lat,
            BigDecimal lng,
            LocalDateTime expectTime,
            Boolean insured,
            BigDecimal declaredValue,
            BigDecimal discount
    ) {
    }
}
