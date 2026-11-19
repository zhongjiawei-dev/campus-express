package com.campus.express.wallet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.Result;
import com.campus.express.common.UserContext;
import com.campus.express.wallet.mapper.WithdrawMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final WithdrawMapper withdrawMapper;

    @GetMapping
    public Result<Wallet> overview() {
        return Result.ok(walletService.overview(UserContext.userId()));
    }

    @GetMapping("/flows")
    public Result<List<WalletFlow>> flows(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.ok(walletService.flows(UserContext.userId(), page, size));
    }

    @GetMapping("/withdraws")
    public Result<Page<Withdraw>> withdraws(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(withdrawMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Withdraw>()
                        .eq(Withdraw::getUserId, UserContext.userId())
                        .orderByDesc(Withdraw::getCreatedAt)));
    }

    @PostMapping("/withdraw")
    public Result<Withdraw> withdraw(@RequestBody WithdrawRequest request) {
        return Result.ok(walletService.applyWithdraw(UserContext.userId(),
                request.amount(), request.channel(), request.account()));
    }

    @GetMapping("/limits")
    public Result<Map<String, Object>> limits() {
        return Result.ok(Map.of(
                "min", WalletService.MIN_WITHDRAW,
                "daily", WalletService.DAILY_LIMIT,
                "monthly", WalletService.MONTH_LIMIT));
    }

    public record WithdrawRequest(BigDecimal amount, Integer channel, String account) {
    }
}
