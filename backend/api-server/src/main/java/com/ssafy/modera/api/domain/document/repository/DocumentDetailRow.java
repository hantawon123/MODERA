package com.ssafy.modera.api.domain.document.repository;

import java.time.OffsetDateTime;

/** 8-3 상세 한 행. 마크다운 본문(content)까지 포함한다. */
public record DocumentDetailRow(
        Integer documentId,
        String name,
        String summary,
        String content,
        int imageCount,
        int delImageCount,
        OffsetDateTime updatedAt
) {
}
