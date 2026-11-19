package com.campus.express.order;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.Result;
import com.campus.express.common.UserContext;
import com.campus.express.complaint.Complaint;
import com.campus.express.order.grab.OrderGrabService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderGrabService grabService;

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody OrderService.CreateRequest request) {
        return Result.ok(orderService.create(UserContext.userId(), request));
    }

    @GetMapping
    public Result<IPage<Map<String, Object>>> list(@RequestParam(defaultValue = "all") String group,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.listMine(UserContext.userId(), group, page, size));
    }

    @GetMapping("/hall")
    public Result<IPage<Map<String, Object>>> hall(@RequestParam(required = false) String sort,
                                                  @RequestParam(required = false) Long stationId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.hall(sort, stationId, page, size));
    }

    @GetMapping("/{orderNo}")
    public Result<Map<String, Object>> detail(@PathVariable String orderNo) {
        return Result.ok(orderService.detail(orderNo, UserContext.userId(), UserContext.role()));
    }

    @PostMapping("/{orderNo}/pay")
    public Result<Order> pay(@PathVariable String orderNo) {
        return Result.ok(orderService.pay(orderNo, UserContext.userId()));
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<Order> cancel(@PathVariable String orderNo, @RequestBody(required = false) CancelRequest request) {
        String reason = request == null ? "用户取消" : request.reason();
        return Result.ok(orderService.cancel(orderNo, UserContext.userId(), reason, false));
    }

    @PostMapping("/{orderNo}/confirm")
    public Result<Order> confirm(@PathVariable String orderNo) {
        return Result.ok(orderService.confirm(orderNo, UserContext.userId()));
    }

    @PostMapping("/{orderNo}/grab")
    public Result<Order> grab(@PathVariable String orderNo) {
        return Result.ok(grabService.grab(orderNo, UserContext.userId()));
    }

    @PostMapping("/{orderNo}/pick")
    public Result<Order> pick(@PathVariable String orderNo, @RequestBody PickRequest request) {
        return Result.ok(orderService.pick(orderNo, UserContext.userId(),
                request.pickupCode(), request.photoUrl()));
    }

    @PostMapping("/{orderNo}/deliver")
    public Result<Order> deliver(@PathVariable String orderNo, @RequestBody DeliverRequest request) {
        return Result.ok(orderService.deliver(orderNo, UserContext.userId(), request.photoUrl()));
    }

    @PostMapping("/{orderNo}/appeal")
    public Result<Complaint> appeal(@PathVariable String orderNo, @RequestBody AppealRequest request) {
        return Result.ok(orderService.appeal(orderNo, UserContext.userId(),
                request.type() == null ? 1 : request.type(),
                request.description(), request.demand(), request.images()));
    }

    public record CancelRequest(String reason) {
    }

    public record PickRequest(String pickupCode, String photoUrl) {
    }

    public record DeliverRequest(String photoUrl) {
    }

    public record AppealRequest(Integer type, String description, String demand, String images) {
    }
}
