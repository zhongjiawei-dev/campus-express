package com.campus.express.order;

import com.campus.express.common.LoginUser;

public record Operator(Long accountId, String role, Long courierId) {

    public static Operator user(Long userId) {
        return new Operator(userId, LoginUser.USER, null);
    }

    public static Operator courier(Long userId, Long courierId) {
        return new Operator(userId, LoginUser.COURIER, courierId);
    }

    public static Operator admin(Long adminId) {
        return new Operator(adminId, LoginUser.ADMIN, null);
    }
}
