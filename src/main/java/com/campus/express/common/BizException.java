package com.campus.express.common;

import lombok.Getter;

import java.util.UUID;

@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public static String traceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
