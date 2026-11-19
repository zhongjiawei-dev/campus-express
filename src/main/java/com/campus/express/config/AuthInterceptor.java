package com.campus.express.config;

import com.campus.express.common.BizException;
import com.campus.express.common.ErrorCode;
import com.campus.express.common.JwtUtil;
import com.campus.express.common.LoginUser;
import com.campus.express.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        LoginUser user = jwtUtil.parse(authorization.substring(7));
        UserContext.set(user);

        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/admin") && !LoginUser.ADMIN.equals(user.role())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅管理员可访问");
        }
        if (uri.startsWith("/api/v1/courier")
                && !LoginUser.COURIER.equals(user.role())
                && !LoginUser.ADMIN.equals(user.role())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅认证代取员可访问");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
