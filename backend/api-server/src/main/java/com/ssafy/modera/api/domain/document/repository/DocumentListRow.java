package com.ssafy.modera.api.domain.document.repository;

import java.time.OffsetDateTime;

/**
 * 8-1 목록 한 행. content는 담지 않는다 — 마크다운 전문이 수십 KB까지 갈 수 있어
 * 목록에 실으면 페이지 하나가 통째로 무거워진다(본문은 8-3에서 준다).
 */
public record DocumentListRow(
        Integer documentId,
        String name,
        String summary,
        int imageCount,
        int delImageCount,
        OffsetDateTime updatedAt
) {
}
