package com.ssafy.modera.api.domain.document.repository;

/** 문서 전체 상세 조회에서 문서별 구성 이미지 ID를 묶기 위한 조회 행. */
public record DocumentImageIdRow(
        Integer documentId,
        Integer imageId
) {
}
