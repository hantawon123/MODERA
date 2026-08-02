package com.ssafy.modera.api.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushTokenRegisterRequest(
        @Schema(description = "로그인과 토큰 관리에 사용하는 기기 식별자")
        @NotBlank
        @Size(max = 64)
        String deviceId,

        @Schema(description = "Firebase Cloud Messaging 등록 토큰")
        @NotBlank
        @Size(max = 4096)
        String fcmToken
) {
}
