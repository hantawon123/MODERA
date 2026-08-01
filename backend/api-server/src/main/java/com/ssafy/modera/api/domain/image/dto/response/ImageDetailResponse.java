package com.ssafy.modera.api.domain.image.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

public record ImageDetailResponse(
        @Schema(description = "이미지 ID") Integer imageId,
        @Schema(description = "원본 이미지 Presigned GET URL") String imageUrl,
        @Schema(description = "썸네일 Presigned GET URL") String thumbnailUrl,
        @Schema(description = "제목") String title,
        @Schema(description = "즐겨찾기 여부") Boolean favorite,
        @Schema(description = "분석 요약") String summary,
        @Schema(description = "카테고리 이름") String category,
        @Schema(description = "태그 이름 목록") List<String> tags,
        @Schema(description = "핵심 정보 목록") List<String> keyInformation,
        @Schema(description = "구조화 데이터") JsonNode structuredData,
        @Schema(description = "최초 등록 시 저장된 OCR 원문") String ocrRawText,
        @Schema(description = "이미지 업로드 일시") OffsetDateTime uploadedAt,
        @Schema(description = "문서화 여부") Boolean isDocumented,
        @Schema(description = "일정 등록 여부") Boolean isCalendared
) {
}
