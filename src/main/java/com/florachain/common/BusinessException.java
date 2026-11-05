package com.florachain.common;

/** 业务异常：携带业务错误码，由接口层统一转换为响应。 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
