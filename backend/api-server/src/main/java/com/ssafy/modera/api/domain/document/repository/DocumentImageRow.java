package com.ssafy.modera.api.domain.document.repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 8-4 문서 구성 이미지 한 행.
 *
 * <p>favorite·category는 document_image_view에 없어서 담지 않는다 — 이 화면은 "이 문서가
 * 어떤 스크린샷으로 만들어졌는지"를 보여주는 용도라, 굳이 user_image_view를 조인해
 * 목록 조회(5-1)와 필드를 맞출 이유가 없다.
 *
 * @param addedAt 문서에 포함된 시각(image_document.updated_at 복사본)
 */
public record DocumentImageRow(
        Integer imageId,
        String title,
        String summary,
        String thumbnailKey,
        List<String> tags,
        OffsetDateTime addedAt
) {
}
