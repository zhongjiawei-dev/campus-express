package com.campus.express.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.Result;
import com.campus.express.common.UserContext;
import com.campus.express.complaint.Complaint;
import com.campus.express.courier.Courier;
import com.campus.express.courier.CourierService;
import com.campus.express.order.Order;
import com.campus.express.order.OrderService;
import com.campus.express.pricing.PriceRule;
import com.campus.express.wallet.Withdraw;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final OrderService orderService;
    private final CourierService courierService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return Result.ok(adminService.login(request.username(), request.password()));
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(adminService.dashboard());
    }

    @GetMapping("/orders")
    public Result<Page<Order>> orders(@RequestParam(required = false) String status,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminService.orders(status, keyword, page, size));
    }

    @PostMapping("/orders/{orderNo}/cancel")
    public Result<Order> cancelOrder(@PathVariable String orderNo,
                                     @RequestBody(required = false) CancelRequest request) {
        String reason = request == null ? "平台介入取消" : request.reason();
        return Result.ok(orderService.cancel(orderNo, UserContext.userId(), reason, true));
    }

    @GetMapping("/users")
    public Result<IPage<Map<String, Object>>> users(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminService.users(page, size));
    }

    @GetMapping("/couriers")
    public Result<Page<Courier>> couriers(@RequestParam(required = false) Integer auditStatus,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminService.couriers(auditStatus, page, size));
    }

    @PostMapping("/couriers/{id}/audit")
    public Result<Void> auditCourier(@PathVariable Long id, @RequestBody AuditRequest request) {
        courierService.audit(id, Boolean.TRUE.equals(request.pass()), request.remark());
        return Result.ok();
    }

    @GetMapping("/price-rules")
    public Result<List<PriceRule>> priceRules() {
        return Result.ok(adminService.priceRules());
    }

    @PostMapping("/price-rules")
    public Result<PriceRule> savePriceRule(@RequestBody PriceRule rule) {
        return Result.ok(adminService.savePriceRule(rule));
    }

    @GetMapping("/withdraws")
    public Result<Page<Withdraw>> withdraws(@RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminService.withdraws(status, page, size));
    }

    @PostMapping("/withdraws/{id}/audit")
    public Result<Void> auditWithdraw(@PathVariable Long id, @RequestBody AuditRequest request) {
        adminService.auditWithdraw(id, Boolean.TRUE.equals(request.pass()),
                request.remark(), UserContext.userId());
        return Result.ok();
    }

    @GetMapping("/complaints")
    public Result<Page<Complaint>> complaints(@RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminService.complaints(status, page, size));
    }

    @PostMapping("/complaints/{id}/resolve")
    public Result<Complaint> resolve(@PathVariable Long id, @RequestBody ResolveRequest request) {
        return Result.ok(adminService.resolveComplaint(id, request.result(),
                request.compensation(), request.responsibility(), UserContext.userId()));
    }

    @PostMapping("/notices")
    public Result<Void> notice(@RequestBody NoticeRequest request) {
        adminService.broadcast(request.title(), request.content());
        return Result.ok();
    }

    public record LoginRequest(String username, String password) {
    }

    public record AuditRequest(Boolean pass, String remark) {
    }

    public record ResolveRequest(String result, BigDecimal compensation, Integer responsibility) {
    }

    public record NoticeRequest(String title, String content) {
    }

    public record CancelRequest(String reason) {
    }
}
