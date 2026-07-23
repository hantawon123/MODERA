package com.ssafy.modera.api.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ImageRegisterResponse(
        @Schema(description = "발급된 이미지 ID") UUID imageId,

        @Schema(description = "이 URL로 파일을 직접 PUT한다(binary, multipart 아님). expiresAt 이후 만료")
        String presignedPutUrl,

        @Schema(description = "MinIO 안에서의 객체 키. 서버 내부용으로 응답에는 참고용으로만 포함")
        String s3Key,

        @Schema(description = "presignedPutUrl 만료 시각") OffsetDateTime expiresAt
) {
}
