package com.campus.express.common;

public record LoginUser(Long userId, String role) {

    public static final String USER = "USER";
    public static final String COURIER = "COURIER";
    public static final String ADMIN = "ADMIN";
}
