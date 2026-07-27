package com.ssafy.modera.api.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @Schema(description = "카카오 인가 서버가 redirect URI로 전달한 일회용 인가 코드")
        @NotBlank String authorizationCode,

        @Schema(description = "Refresh Token을 구분할 클라이언트 기기 식별자", example = "android-device-uuid-1234")
        @NotBlank String deviceId
) {
}
