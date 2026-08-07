package com.ssafy.modera.api.domain.document.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 8-4 문서 구성 이미지 항목("이 문서가 어떤 스크린샷으로 만들어졌는지").
 *
 * <p>목록 조회(5-1)와 달리 favorite·category가 없다 — 조회 모델
 * (query_schema.document_image_view)에 없는 값이라 굳이 조인해 채우지 않았다.
 */
public record DocumentImageResponse(
        @Schema(description = "이미지 ID", example = "1024") Integer imageId,
        @Schema(description = "이미지 제목") String title,
        @Schema(description = "이미지 요약") String summary,
        @Schema(description = "썸네일 presigned GET URL(1시간 유효). 썸네일이 없으면 null")
        String thumbnailUrl,
        @Schema(description = "태그 이름 목록") List<String> tags,
        @Schema(description = "문서에 포함된 시각") OffsetDateTime addedAt
) {
}
