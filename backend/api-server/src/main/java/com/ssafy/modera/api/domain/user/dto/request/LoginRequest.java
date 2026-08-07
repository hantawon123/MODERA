package com.ssafy.modera.api.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "로그인 아이디", example = "moderauser")
        @NotBlank String loginId,

        @Schema(description = "비밀번호", example = "password123")
        @NotBlank String password,

        @Schema(description = "기기 식별자. deviceId 단위로 refreshToken이 관리된다(같은 기기 재로그인 시 교체)",
                example = "android-device-uuid-1234")
        @NotBlank String deviceId
) {
}
