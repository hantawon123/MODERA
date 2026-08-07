package com.ssafy.modera.api.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 5-7 문서화 관련 자료 검색 요청.
 *
 * <p>문서를 만드는 요청이 아니다 — 사용자가 고른 기준 이미지들과 내용이 비슷한 다른
 * 이미지를 추천받는 검색이라, 멱등키(clientRequestId)가 없다. 몇 번을 불러도 조회만
 * 일어난다.
 */
public record ImageDocumentizeRequest(
        @Schema(description = "관련 자료 검색의 기준이 되는 본인 소유 이미지 ID 목록", example = "[1024, 1025]")
        @NotEmpty(message = "imageIds는 1개 이상이어야 합니다.")
        List<Integer> imageIds
) {
}
