package com.ssafy.modera.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답 (3-2). refreshToken 은 쿠키가 아닌 body 로 전달한다.")
public record LoginResponse(

        @Schema(description = "Access Token (만료 30분)", example = "eyJhbGciOi...")
        String accessToken,

        @Schema(description = "Refresh Token (만료 14일)", example = "eyJhbGciOi...")
        String refreshToken,

        @Schema(description = "사용자 ID", example = "1")
        Long userId
) {

    public static LoginResponse of(String accessToken, String refreshToken, Long userId) {
        return new LoginResponse(accessToken, refreshToken, userId);
    }
}
