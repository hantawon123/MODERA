package com.ssafy.modera.api.global.response;

import com.ssafy.modera.api.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * /api/v1/** 응답 envelope. /internal/**, actuator, swagger에는 적용하지 않는다.
 */
public record ApiResponse<T>(
        @Schema(description = "성공 여부", example = "SUCCESS", allowableValues = {"SUCCESS", "FAIL"}) String result,
        @Schema(description = "성공은 SUCCESS 고정, 실패는 에러코드 문자열(예: IMAGE_NOT_FOUND)", example = "SUCCESS") String code,
        @Schema(description = "사람이 읽는 메시지", example = "요청이 성공했습니다.") String message,
        @Schema(description = "성공 시 실제 응답 데이터, 실패 시 보통 null(검증 실패 400은 필드 오류 배열)") T data,
        @Schema(description = "ISO-8601 UTC", example = "2026-07-23T06:00:00.000Z") String timestamp
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
