package com.ssafy.modera.api.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 5-8/5-9 이미지 파일 URL 발급 응답. 앱이 들고 있던 presigned URL이 만료됐을 때
 * 이 API로 새 URL만 받아 재시도한다(302 리다이렉트가 아니라 값으로 준다 — 안드로이드
 * 요청 반영).
 */
public record ImageFileUrlResponse(
        @Schema(description = "presigned GET URL(1시간 유효). 만료되므로 영구 저장하지 말 것")
        String url
) {
}
