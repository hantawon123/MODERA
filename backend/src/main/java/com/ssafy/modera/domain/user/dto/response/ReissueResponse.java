package com.ssafy.modera.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 재발급 응답 (3-3). refreshToken 도 함께 회전 발급된다.")
public record ReissueResponse(

        @Schema(description = "새 Access Token", example = "eyJhbGciOi...")
        String accessToken,

        @Schema(description = "새 Refresh Token (기존 토큰은 즉시 폐기)", example = "eyJhbGciOi...")
        String refreshToken
) {

    public static ReissueResponse of(String accessToken, String refreshToken) {
        return new ReissueResponse(accessToken, refreshToken);
    }
}
