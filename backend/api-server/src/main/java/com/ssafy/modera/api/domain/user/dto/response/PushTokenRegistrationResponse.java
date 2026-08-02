package com.ssafy.modera.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PushTokenRegistrationResponse(
        @Schema(description = "등록 또는 갱신된 기기 식별자")
        String deviceId
) {
}
