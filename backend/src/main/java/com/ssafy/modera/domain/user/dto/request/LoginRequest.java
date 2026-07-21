package com.ssafy.modera.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청 (3-2)")
public record LoginRequest(

        @Schema(description = "로그인 ID", example = "newUser123")
        @NotBlank(message = "로그인 ID는 필수입니다.")
        String loginId,

        @Schema(description = "비밀번호", example = "securePassword123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        @Schema(description = "기기 식별자. 생략 시 기본 기기로 처리되며, 같은 기기의 기존 refreshToken 은 회전 폐기된다.",
                example = "android-device-uuid")
        @Size(max = 64, message = "기기 식별자는 64자를 넘을 수 없습니다.")
        String deviceId
) {
}
