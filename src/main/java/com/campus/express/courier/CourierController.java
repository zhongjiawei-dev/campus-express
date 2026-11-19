package com.campus.express.courier;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.Result;
import com.campus.express.common.UserContext;
import com.campus.express.order.OrderService;
import com.campus.express.wallet.WalletFlow;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/courier")
@RequiredArgsConstructor
public class CourierController {

    private final CourierService courierService;
    private final OrderService orderService;

    @PostMapping("/apply")
    public Result<Courier> apply(@RequestBody CourierService.ApplyRequest request) {
        return Result.ok(courierService.apply(UserContext.userId(), request));
    }

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        return Result.ok(courierService.profile(UserContext.userId()));
    }

    @GetMapping("/me")
    public Result<Courier> me() {
        return Result.ok(courierService.findByUser(UserContext.userId()));
    }

    @PutMapping("/settings")
    public Result<Void> settings(@RequestBody SettingsRequest request) {
        courierService.updateSettings(UserContext.userId(), request.workStatus(),
                request.serviceStart(), request.serviceEnd(), request.maxWeightGram(), request.stationIds());
        return Result.ok();
    }

    @GetMapping("/orders")
    public Result<IPage<Map<String, Object>>> orders(@RequestParam(defaultValue = "active") String group,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.listCourier(UserContext.userId(), group, page, size));
    }

    @GetMapping("/income")
    public Result<List<WalletFlow>> income(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.ok(courierService.incomeFlows(UserContext.userId(), page, size));
    }

    public record SettingsRequest(Integer workStatus, String serviceStart, String serviceEnd,
                                  Integer maxWeightGram, String stationIds) {
    }
}
