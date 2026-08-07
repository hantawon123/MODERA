package com.ssafy.modera.api.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @Schema(description = "로그인 또는 이전 재발급으로 받은 refreshToken. 이 호출 이후 즉시 무효화된다")
        @NotBlank String refreshToken,

        @Schema(description = "발급 당시와 같은 deviceId. 다르면 INVALID_REFRESH_TOKEN", example = "android-device-uuid-1234")
        @NotBlank String deviceId
) {
}
