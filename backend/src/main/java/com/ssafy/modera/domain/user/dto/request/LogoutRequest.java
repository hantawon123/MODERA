package com.ssafy.modera.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그아웃 요청 (3-4)")
public record LogoutRequest(

        @Schema(description = "폐기할 refreshToken", example = "eyJhbGciOi...")
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken,

        @Schema(description = "폐기 대상 기기 식별자. 생략 시 기본 기기로 처리한다.", example = "android-device-uuid")
        @Size(max = 64, message = "기기 식별자는 64자를 넘을 수 없습니다.")
        String deviceId
) {
}
