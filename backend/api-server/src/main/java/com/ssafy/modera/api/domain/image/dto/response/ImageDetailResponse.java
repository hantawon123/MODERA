package com.ssafy.modera.api.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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
        @Schema(description = "구조화 데이터(일정·상품 등 type별 fields)") Map<String, Object> scheduledData,
        @Schema(description = "AI가 정제한 OCR 텍스트(정제본이 없으면 null)") String ocrRefinedText,
        @Schema(description = "이미지 업로드 일시") OffsetDateTime uploadedAt,
        @Schema(description = "문서화 여부") Boolean isDocumented,
        @Schema(description = "일정 등록 여부") Boolean isCalendared
) {
}
