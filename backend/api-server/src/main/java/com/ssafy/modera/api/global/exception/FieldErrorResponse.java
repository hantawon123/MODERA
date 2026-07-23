package com.ssafy.modera.api.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;

public record FieldErrorResponse(
        @Schema(description = "검증에 실패한 요청 필드명", example = "email") String field,
        @Schema(description = "실패 사유", example = "must not be blank") String message
) {
}
