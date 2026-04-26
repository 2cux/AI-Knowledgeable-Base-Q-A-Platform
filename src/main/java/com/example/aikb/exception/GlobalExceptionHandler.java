package com.example.aikb.exception;

import com.example.aikb.common.Result;
import jakarta.validation.ConstraintViolationException;
import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 全局异常处理器，统一返回结构化的 JSON 错误响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception, code={}, httpStatus={}, message={}",
                ex.getCode(), ex.getHttpStatus(), ex.getMessage());
        return build(ex.getHttpStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return build(HttpStatus.BAD_REQUEST.value(), 40001,
                extractFieldErrorMessage(ex.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException ex) {
        return build(HttpStatus.BAD_REQUEST.value(), 40001,
                extractFieldErrorMessage(ex.getBindingResult().getFieldErrors()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST.value(), 40001, normalizeMessage(ex.getMessage(), "参数校验失败"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST.value(), 40001, ex.getName() + " 参数类型错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        return build(HttpStatus.BAD_REQUEST.value(), 40001, "缺少必填参数: " + ex.getParameterName());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Result<Void>> handleMissingServletRequestPartException(MissingServletRequestPartException ex) {
        String message = "file".equals(ex.getRequestPartName())
                ? "文件不能为空"
                : "缺少上传参数: " + ex.getRequestPartName();
        return build(HttpStatus.BAD_REQUEST.value(), 40001, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST.value(), 40001, "请求体格式错误或缺少必要字段");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST.value(), 40001, normalizeMessage(ex.getMessage(), "参数不合法"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result<Void>> handleIllegalStateException(IllegalStateException ex) {
        return build(HttpStatus.BAD_REQUEST.value(), 40001, normalizeMessage(ex.getMessage(), "状态不允许当前操作"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN.value(), 40300, "无权访问当前资源");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthenticationException(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED.value(), 40100, "用户未登录或认证失败");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE.value(), 41300, "文件大小超过限制");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Result<Void>> handleMultipartException(MultipartException ex) {
        String rawMessage = ex.getMessage();
        String message = rawMessage != null && rawMessage.contains("multipart")
                ? "文件上传请求格式错误，请使用 multipart/form-data"
                : "文件上传失败";
        return build(HttpStatus.BAD_REQUEST.value(), 40001, message);
    }

    @ExceptionHandler({FileNotFoundException.class, NoSuchFileException.class})
    public ResponseEntity<Result<Void>> handleFileNotFoundException(Exception ex) {
        return build(HttpStatus.NOT_FOUND.value(), 40400, "文件不存在或已被删除，请重新上传");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR.value(), 50000, "系统异常，请稍后重试");
    }

    private ResponseEntity<Result<Void>> build(int httpStatus, int code, String message) {
        return ResponseEntity.status(httpStatus)
                .body(Result.fail(code, normalizeMessage(message, "系统异常，请稍后重试")));
    }

    private String extractFieldErrorMessage(List<FieldError> fieldErrors) {
        String message = fieldErrors.stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return normalizeMessage(message, "参数校验失败");
    }

    private String normalizeMessage(String message, String defaultMessage) {
        if (message == null || message.isBlank()) {
            return defaultMessage;
        }
        return message;
    }
}
