package com.ssafy.modera.api.domain.image.repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 문서 생성 이벤트에 실을 이미지 재료. {@code query_schema.user_image_view} 한 행에서
 * 뽑는다.
 *
 * <p>목록용 {@link UserImageViewSummary}와 필드가 겹치지만 따로 두는 이유는 용도가
 * 달라서다 — 이쪽은 화면에 안 쓰는 keyInformation·uploadedAt이 필요하고, 대신 썸네일·
 * 즐겨찾기는 필요 없다. 목록 DTO를 넓히면 5-1 응답에 안 쓰는 컬럼을 계속 실어 나르게 된다.
 *
 * @param analysisStatus 분석 완료 검증용. 화면에 나가지 않고 서비스 계층에서만 본다.
 */
public record DocumentSourceImage(
        Integer imageId,
        String title,
        String summary,
        String categoryName,
        List<String> tagNames,
        List<String> keyInformation,
        OffsetDateTime uploadedAt,
        String analysisStatus
) {
}
