package com.ssafy.modera.api.domain.user.dto.response;

/** login과 refresh 둘 다 "새 토큰 쌍 + userId"라 응답 모양이 같아 하나로 공유한다. */
public record TokenResponse(String accessToken, String refreshToken, Long userId) {
}
