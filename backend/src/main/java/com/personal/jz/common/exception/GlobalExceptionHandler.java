package com.personal.jz.common.exception;

import com.personal.jz.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> biz(BizException e, HttpServletRequest req) {
        log.warn("[BizException] {} {} -> code={} msg={}", req.getMethod(), req.getRequestURI(), e.getCode(), e.getMessage());
        HttpStatus status = mapStatus(e.getCode());
        return ResponseEntity.status(status).body(ApiResponse.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::fieldToString)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCodeEnums.PARAM_INVALID.getCode(), msg));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> missingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ErrorCodeEnums.PARAM_INVALID.getCode(),
                "missing param: " + e.getParameterName()));
    }

    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponse<Void>> auth(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ErrorCodeEnums.UNAUTHORIZED.getCode(), e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ErrorCodeEnums.FORBIDDEN.getCode(), e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> all(Exception e, HttpServletRequest req) {
        log.error("[Unhandled] {} {}", req.getMethod(), req.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCodeEnums.INTERNAL_ERROR));
    }

    private HttpStatus mapStatus(int code) {
        if (code == ErrorCodeEnums.UNAUTHORIZED.getCode()) return HttpStatus.UNAUTHORIZED;
        if (code == ErrorCodeEnums.FORBIDDEN.getCode()) return HttpStatus.FORBIDDEN;
        if (code == ErrorCodeEnums.NOT_FOUND.getCode()) return HttpStatus.NOT_FOUND;
        if (code == ErrorCodeEnums.CONFLICT.getCode() || code == ErrorCodeEnums.USERNAME_TAKEN.getCode()
                || code == ErrorCodeEnums.BOOK_NAME_TAKEN.getCode() || code == ErrorCodeEnums.CATEGORY_NAME_TAKEN.getCode()
                || code == ErrorCodeEnums.CATEGORY_IN_USE.getCode())
            return HttpStatus.CONFLICT;
        if (code == ErrorCodeEnums.ACCOUNT_LOCKED.getCode()) return HttpStatus.LOCKED;
        if (code == ErrorCodeEnums.RATE_LIMITED.getCode()) return HttpStatus.TOO_MANY_REQUESTS;
        if (code >= 70001 && code <= 70999) return HttpStatus.BAD_REQUEST;
        return HttpStatus.OK;
    }

    private String fieldToString(FieldError fe) {
        return fe.getField() + " " + (fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage());
    }
}
