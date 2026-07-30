package com.ssafy.modera.api.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 연관 이미지 목록 아이템. 필드명은 5-1 목록(GET /api/v1/images) 아이템과 맞췄고,
 * 유사도 score 하나만 더 붙는다(uploadedAt은 유사도순 정렬이라 의미가 없어 뺐다).
 */
public record SimilarImageItemResponse(
        @Schema(description = "이미지 ID", example = "7") Integer imageId,
        @Schema(description = "제목", example = "C++ 프로그래밍 입문") String title,
        @Schema(description = "요약", example = "교보문고에서 판매 중인 C++ 프로그래밍 입문서, 32,000원") String summary,
        @Schema(description = "즐겨찾기 여부", example = "false") Boolean favorite,
        @Schema(description = "썸네일 presigned GET URL. 썸네일이 아직 없으면 null") String thumbnailUrl,
        @Schema(description = "태그 이름 목록", example = "[\"C++\", \"쇼핑\"]") List<String> tags,
        @Schema(description = "카테고리 이름", example = "공부") String category,
        @Schema(description = "기준 이미지와의 유사도(1에 가까울수록 유사)", example = "0.93") Float score
) {
}
