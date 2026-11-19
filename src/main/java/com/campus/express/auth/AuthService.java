package com.campus.express.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;
import com.campus.express.common.JwtUtil;
import com.campus.express.common.LoginUser;
import com.campus.express.courier.Courier;
import com.campus.express.courier.mapper.CourierMapper;
import com.campus.express.user.User;
import com.campus.express.user.mapper.UserMapper;
import com.campus.express.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final CourierMapper courierMapper;
    private final JwtUtil jwtUtil;
    private final WalletService walletService;

    /**
     * 演示环境的模拟第三方登录：code → 登录标识。
     * 生产环境应接入学校统一身份认证或账号密码登录，禁止信任前端传入的用户标识。
     */
    @Transactional
    public Map<String, Object> login(String code, String nickname) {
        if (code == null || code.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少登录凭证 code");
        }
        String openId = "wx_" + code.trim();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, openId));
        if (user == null) {
            user = new User();
            user.setOpenId(openId);
            user.setNickname(nickname == null || nickname.isBlank() ? "校园同学" : nickname);
            user.setStatus(0);
            user.setCreditScore(100);
            user.setInviteCode(randomCode());
            userMapper.insert(user);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BizException(ErrorCode.USER_DISABLED);
        }
        walletService.getOrCreate(user.getId());

        Courier courier = courierMapper.selectOne(new LambdaQueryWrapper<Courier>()
                .eq(Courier::getUserId, user.getId()));
        String role = courier != null && courier.getAuditStatus() != null
                && courier.getAuditStatus() == Courier.AUDIT_APPROVED
                ? LoginUser.COURIER : LoginUser.USER;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtUtil.generate(user.getId(), role));
        result.put("userId", user.getId());
        result.put("role", role);
        result.put("nickname", user.getNickname());
        result.put("courierStatus", courier == null ? 0 : courier.getAuditStatus());
        return result;
    }

    public Map<String, Object> me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        result.put("phoneMask", user.getPhoneMask());
        result.put("studentNo", user.getStudentNo());
        result.put("creditScore", user.getCreditScore());
        result.put("inviteCode", user.getInviteCode());
        return result;
    }

    private String randomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }
}
