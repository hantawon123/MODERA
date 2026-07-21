package com.ssafy.modera.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급 요청 (3-3)")
public record ReissueRequest(

        @Schema(description = "현재 보유한 refreshToken", example = "eyJhbGciOi...")
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken
) {
}
