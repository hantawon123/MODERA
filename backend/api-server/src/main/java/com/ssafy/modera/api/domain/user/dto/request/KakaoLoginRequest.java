package com.ssafy.modera.api.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KakaoLoginRequest(
        @Schema(description = "Android Kakao SDK가 발급한 Kakao Access Token")
        @NotBlank
        @Size(max = 4096)
        String kakaoAccessToken,

        @Schema(description = "Refresh Token을 구분할 클라이언트 기기 식별자",
                example = "android-device-uuid-1234")
        @NotBlank
        @Size(max = 64)
        String deviceId
) {
}
