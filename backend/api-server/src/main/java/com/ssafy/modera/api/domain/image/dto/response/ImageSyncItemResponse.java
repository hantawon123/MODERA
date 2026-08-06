package com.ssafy.modera.api.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 5-10 전체 동기화 항목. 필드명은 상세 조회(5-2)와 동일하게 맞춰 앱이 같은 매퍼로
 * Room 엔티티를 채울 수 있게 한다. presigned URL(imageUrl/thumbnailUrl)만 없다 —
 * 만료되는 값이라 로컬 DB에 저장할 수 없고, 이미지는 5-8/5-9로 그때그때 받는다.
 */
public record ImageSyncItemResponse(
        @Schema(description = "이미지 ID") Integer imageId,
        @Schema(description = "제목") String title,
        @Schema(description = "즐겨찾기 여부") Boolean favorite,
        @Schema(description = "분석 요약") String summary,
        @Schema(description = "카테고리 아이디") Integer categoryId,
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
