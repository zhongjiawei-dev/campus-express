package com.campus.express.wallet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;
import com.campus.express.common.MaskUtil;
import com.campus.express.wallet.mapper.WalletFlowMapper;
import com.campus.express.wallet.mapper.WalletMapper;
import com.campus.express.wallet.mapper.WithdrawMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class WalletService {

    public static final BigDecimal MIN_WITHDRAW = new BigDecimal("10.00");
    public static final BigDecimal DAILY_LIMIT = new BigDecimal("500.00");
    public static final BigDecimal MONTH_LIMIT = new BigDecimal("2000.00");

    private final WalletMapper walletMapper;
    private final WalletFlowMapper flowMapper;
    private final WithdrawMapper withdrawMapper;

    public Wallet getOrCreate(Long userId) {
        Wallet wallet = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        if (wallet != null) {
            return wallet;
        }
        wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setFrozenAmount(BigDecimal.ZERO);
        wallet.setTotalIncome(BigDecimal.ZERO);
        wallet.setTotalExpense(BigDecimal.ZERO);
        wallet.setVersion(0);
        wallet.setStatus(0);
        try {
            walletMapper.insert(wallet);
            return wallet;
        } catch (Exception e) {
            Wallet existed = walletMapper.selectOne(new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
            if (existed != null) {
                return existed;
            }
            throw e;
        }
    }

    /** 扣款：带版本号的 CAS，最多重试 3 次 */
    @Transactional
    public BigDecimal debit(Long userId, BigDecimal amount, int bizType, String bizNo, String remark) {
        if (amount == null || amount.signum() <= 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "金额必须大于 0");
        }
        Wallet wallet = getOrCreate(userId);
        for (int i = 0; i < 3; i++) {
            int rows = walletMapper.debit(wallet.getId(), amount, wallet.getVersion());
            if (rows == 1) {
                BigDecimal after = wallet.getBalance().subtract(amount);
                insertFlow(wallet, 2, bizType, amount, after, bizNo, remark);
                return after;
            }
            wallet = walletMapper.selectById(wallet.getId());
        }
        throw new BizException(ErrorCode.BALANCE_NOT_ENOUGH);
    }

    @Transactional
    public BigDecimal credit(Long userId, BigDecimal amount, int bizType, String bizNo, String remark) {
        if (amount == null || amount.signum() <= 0) {
            return null;
        }
        Wallet wallet = getOrCreate(userId);
        for (int i = 0; i < 3; i++) {
            int rows = walletMapper.credit(wallet.getId(), amount, wallet.getVersion());
            if (rows == 1) {
                BigDecimal after = wallet.getBalance().add(amount);
                insertFlow(wallet, 1, bizType, amount, after, bizNo, remark);
                return after;
            }
            wallet = walletMapper.selectById(wallet.getId());
        }
        throw new BizException(ErrorCode.SYSTEM_BUSY, "钱包繁忙，请稍后重试");
    }

    public Wallet overview(Long userId) {
        return getOrCreate(userId);
    }

    public List<WalletFlow> flows(Long userId, int page, int size) {
        Page<WalletFlow> result = flowMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<WalletFlow>()
                        .eq(WalletFlow::getUserId, userId)
                        .orderByDesc(WalletFlow::getCreatedAt));
        return result.getRecords();
    }

    @Transactional
    public Withdraw applyWithdraw(Long userId, BigDecimal amount, Integer channel, String account) {
        if (amount == null || amount.compareTo(MIN_WITHDRAW) < 0) {
            throw new BizException(ErrorCode.WITHDRAW_BELOW_MIN, "单次提现不低于 10 元");
        }
        if (amount.compareTo(DAILY_LIMIT) > 0) {
            throw new BizException(ErrorCode.WITHDRAW_LIMIT, "单日提现上限 500 元");
        }
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        if (withdrawMapper.sumSince(userId, todayStart).compareTo(DAILY_LIMIT) >= 0) {
            throw new BizException(ErrorCode.WITHDRAW_LIMIT, "今日提现额度已用完");
        }
        if (withdrawMapper.sumSince(userId, monthStart).add(amount).compareTo(MONTH_LIMIT) > 0) {
            throw new BizException(ErrorCode.WITHDRAW_LIMIT, "本月提现上限 2000 元");
        }

        Withdraw withdraw = new Withdraw();
        withdraw.setWithdrawNo(genNo("W"));
        withdraw.setUserId(userId);
        withdraw.setAmount(amount);
        withdraw.setFee(BigDecimal.ZERO);
        withdraw.setActualAmount(amount);
        withdraw.setChannel(channel == null ? 1 : channel);
        withdraw.setAccountMask(MaskUtil.tail(account, 4));
        withdraw.setStatus(1);

        debit(userId, amount, 5, withdraw.getWithdrawNo(), "提现申请");
        withdrawMapper.insert(withdraw);
        return withdraw;
    }

    @Transactional
    public void auditWithdraw(Long withdrawId, boolean pass, String remark, Long adminId) {
        Withdraw withdraw = withdrawMapper.selectById(withdrawId);
        if (withdraw == null || withdraw.getStatus() != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "提现单不存在或已处理");
        }
        if (pass) {
            withdraw.setStatus(2);
            withdrawMapper.updateById(withdraw);
            return;
        }
        withdraw.setStatus(3);
        withdrawMapper.updateById(withdraw);
        credit(withdraw.getUserId(), withdraw.getAmount(), 6, withdraw.getWithdrawNo(), "提现驳回退回");
    }

    private void insertFlow(Wallet wallet, int direction, int bizType, BigDecimal amount,
                            BigDecimal after, String bizNo, String remark) {
        WalletFlow flow = new WalletFlow();
        flow.setFlowNo(genNo("F"));
        flow.setWalletId(wallet.getId());
        flow.setUserId(wallet.getUserId());
        flow.setBizType(bizType);
        flow.setDirection(direction);
        flow.setAmount(amount);
        flow.setBalanceAfter(after);
        flow.setBizNo(bizNo);
        flow.setStatus(2);
        flow.setRemark(remark);
        flowMapper.insert(flow);
    }

    private String genNo(String prefix) {
        return prefix + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
