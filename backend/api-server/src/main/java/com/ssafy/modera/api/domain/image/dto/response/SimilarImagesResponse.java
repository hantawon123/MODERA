package com.ssafy.modera.api.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 연관 자료 화면 응답. 목록이지만 페이지네이션이 없어서 PageResponse를 쓰지 않고,
 * 목록 필드명(list)만 맞춘다.
 */
public record SimilarImagesResponse(
        @Schema(description = "기준 이미지 ID", example = "20") Integer baseImageId,
        @Schema(description = "기준 이미지 제목. 분석 전이면 null", example = "ASCII 해커톤") String baseTitle,
        @Schema(description = "list의 길이", example = "3") Integer count,
        @Schema(description = "유사도 내림차순 목록") List<SimilarImageItemResponse> list
) {

    public static SimilarImagesResponse of(
            Integer baseImageId,
            String baseTitle,
            List<SimilarImageItemResponse> list
    ) {
        return new SimilarImagesResponse(baseImageId, baseTitle, list.size(), list);
    }
}
