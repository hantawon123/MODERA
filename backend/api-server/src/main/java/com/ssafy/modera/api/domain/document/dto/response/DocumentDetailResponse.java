package com.ssafy.modera.api.domain.document.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 8-3 문서 상세.
 *
 * @param regenerating 재분석이 진행 중이면 true. 이 값이 true인 동안 클라이언트는 로딩
 *                     화면을 띄우고 폴링하면 된다. 완료 시 DOCUMENT_REANALYSIS FCM도
 *                     발송된다. 실패로 끝나도 문서
 *                     내용은 이전 상태 그대로 남는다(재분석은 새 문서를 만들지 않는다).
 */
public record DocumentDetailResponse(
        @Schema(description = "문서 ID", example = "101") Integer documentId,
        @Schema(description = "문서 이름") String name,
        @Schema(description = "AI가 만든 문서 요약. 036 이전에 생성된 문서는 null이다.") String summary,
        @Schema(description = "마크다운 본문 전문") String content,
        @Schema(description = "문서를 구성하는 살아 있는 이미지 수") int imageCount,
        @Schema(description = "0이 아니면 원본 이미지가 삭제돼 재분석이 필요하다") int delImageCount,
        @Schema(description = "문서를 구성하는 이미지 ID. 상세 화면의 스크린샷 진입점") List<Integer> imageIds,
        @Schema(description = "재분석 진행 중 여부") boolean regenerating,
        @Schema(description = "마지막 갱신 시각") OffsetDateTime updatedAt
) {
}
