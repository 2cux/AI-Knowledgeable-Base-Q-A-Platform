package com.example.aikb.exception;

import lombok.Getter;

/**
 * 业务异常，统一承载业务码、业务消息和建议返回的 HTTP 状态码。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;
    private final int httpStatus;

    public BusinessException(String message) {
        this(40000, message);
    }

    public BusinessException(int code, String message) {
        this(code, message, resolveHttpStatus(code));
    }

    public BusinessException(int code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    private static int resolveHttpStatus(int code) {
        if (code >= 40100 && code < 40200) {
            return 401;
        }
        if (code >= 40300 && code < 40400) {
            return 403;
        }
        if (code >= 40400 && code < 40500) {
            return 404;
        }
        if (code >= 40900 && code < 41000) {
            return 409;
        }
        if (code >= 41300 && code < 41400) {
            return 413;
        }
        if (code >= 40000 && code < 50000) {
            return 400;
        }
        return 500;
    }
}
