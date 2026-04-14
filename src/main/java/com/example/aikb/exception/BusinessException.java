package com.example.aikb.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(40000, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
