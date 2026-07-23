package com.ssafy.modera.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** login과 refresh 둘 다 "새 토큰 쌍 + userId"라 응답 모양이 같아 하나로 공유한다. */
public record TokenResponse(
        @Schema(description = "API 호출 시 Authorization: Bearer {accessToken}로 쓴다. 30분 후 만료") String accessToken,
        @Schema(description = "만료 시 /api/v1/auth/refresh에 제시해 재발급받는다. 14일 후 만료, 재발급마다 회전됨") String refreshToken,
        @Schema(description = "사용자 ID", example = "1") Integer userId
) {
}
