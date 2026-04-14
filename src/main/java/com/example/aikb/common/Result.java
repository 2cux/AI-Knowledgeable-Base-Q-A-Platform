package com.example.aikb.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return success(null, "OK");
    }

    public static <T> Result<T> success(T data) {
        return success(data, "OK");
    }

    public static <T> Result<T> success(T data, String message) {
        return Result.<T>builder()
                .code(0)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> Result<T> fail(int code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}
