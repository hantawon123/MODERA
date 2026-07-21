package com.ssafy.modera.global.exception;

import com.ssafy.modera.global.domain.ErrorCode;
import com.ssafy.modera.global.domain.dto.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionAdvice {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("[BUS] {} {} | {}: {}", request.getMethod(), request.getRequestURI(), ex.getErrorCode().getCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(CommonResponse.onFailure(ex.getErrorCode()));
    }

    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<CommonResponse<List<CommonResponse.ValidationErrorDetail>>> handleValidationException(BindException ex, HttpServletRequest request) {
        String errorMessage = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "Unknown Validation Error";

        log.warn("[VAL] {} {} | Msg: {}", request.getMethod(), request.getRequestURI(), errorMessage);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(CommonResponse.onFailure(ErrorCode.INVALID_INPUT_VALUE, ex.getBindingResult()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResponse<List<CommonResponse.ValidationErrorDetail>>> handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("[VAL] {} {} | Msg: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());

        List<CommonResponse.ValidationErrorDetail> details = ex.getConstraintViolations().stream()
                .map(violation -> new CommonResponse.ValidationErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .collect(Collectors.toList());

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(CommonResponse.onFailure(ErrorCode.INVALID_INPUT_VALUE, details));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            NoHandlerFoundException.class,
            NoResourceFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<CommonResponse<Void>> handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("[BAD] {} {} | {}: {}", request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage());

        ErrorCode errorCode = switch (ex) {
            case NoHandlerFoundException ignored -> ErrorCode.NOT_FOUND;
            case NoResourceFoundException ignored -> ErrorCode.NOT_FOUND;
            case HttpRequestMethodNotSupportedException ignored -> ErrorCode.METHOD_NOT_ALLOWED;
            case IllegalArgumentException ignored -> ErrorCode.INVALID_INPUT_VALUE;
            default -> ErrorCode.BAD_REQUEST;
        };

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponse.onFailure(errorCode));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonResponse<Void>> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("[AUTH] {} {} | {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getStatus())
                .body(CommonResponse.onFailure(ErrorCode.UNAUTHORIZED));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonResponse<Void>> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[DENY] {} {} | {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.getStatus())
                .body(CommonResponse.onFailure(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        log.error("[ERR] {} {} | Exception: ", request.getMethod(), request.getRequestURI(), ex);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(CommonResponse.onFailure(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
