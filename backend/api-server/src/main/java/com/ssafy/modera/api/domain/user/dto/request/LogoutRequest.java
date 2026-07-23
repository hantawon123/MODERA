package com.ssafy.modera.api.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @Schema(description = "로그아웃시킬 기기의 현재 refreshToken. 저장된 값과 다르면 실패한다")
        @NotBlank String refreshToken,

        @Schema(description = "로그아웃시킬 기기 식별자", example = "android-device-uuid-1234")
        @NotBlank String deviceId
) {
}
