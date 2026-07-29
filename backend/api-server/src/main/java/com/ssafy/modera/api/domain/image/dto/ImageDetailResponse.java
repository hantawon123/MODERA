package com.ssafy.modera.api.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageDetailResponse(
        @Schema(description = "이미지 ID") Integer imageId,
        @Schema(description = "MinIO 안에서의 객체 키") String s3Key,
        @Schema(description = "업로드 상태", example = "UPLOADED", allowableValues = {"PENDING", "UPLOADED"}) String uploadStatus,
        @Schema(description = "분석 상태. worker가 이벤트로 갱신한다", example = "COMPLETED", allowableValues = {"NONE", "PROCESSING", "COMPLETED", "FAILED"}) String analysisStatus,
        @Schema(description = "제목") String title,
        @Schema(description = "즐겨찾기 여부") Boolean favorite
) {
}
