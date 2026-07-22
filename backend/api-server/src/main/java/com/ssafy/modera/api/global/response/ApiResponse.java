package com.ssafy.modera.api.global.response;

import com.ssafy.modera.api.global.exception.ErrorCode;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * /api/v1/** 응답 envelope. /internal/**, actuator, swagger에는 적용하지 않는다.
 */
public record ApiResponse<T>(
        String result,
        String code,
        String message,
        T data,
        String timestamp
) {
    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_FAIL = "FAIL";
    private static final String CODE_SUCCESS = "SUCCESS";
    private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 성공했습니다.";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    public static <T> ApiResponse<T> success(T data) {
        return success(DEFAULT_SUCCESS_MESSAGE, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(RESULT_SUCCESS, CODE_SUCCESS, message, data, now());
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(RESULT_FAIL, errorCode.getCode(), message, data, now());
    }

    private static String now() {
        return TIMESTAMP_FORMATTER.format(Instant.now());
    }
}
