package com.ssafy.modera.api.domain.document.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/** 8-1 목록 항목. 본문(content)은 빠진다 — 목록 카드는 이름과 요약만 보여준다. */
public record DocumentSummaryResponse(
        @Schema(description = "문서 ID", example = "101") Integer documentId,
        @Schema(description = "문서 이름", example = "오사카 3박 4일 여행 계획") String name,
        @Schema(description = "AI가 만든 문서 요약. 036 이전에 생성된 문서는 null이다.") String summary,
        @Schema(description = "문서를 구성하는 살아 있는 이미지 수", example = "8") int imageCount,
        @Schema(description = "0이 아니면 문서 생성 후 원본 이미지가 삭제돼 재분석이 필요하다", example = "0")
        int delImageCount,
        @Schema(description = "마지막 갱신 시각") OffsetDateTime updatedAt
) {
}
