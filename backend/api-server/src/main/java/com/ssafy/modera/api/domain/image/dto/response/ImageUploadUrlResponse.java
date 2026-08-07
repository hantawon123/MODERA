package com.ssafy.modera.api.domain.image.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record ImageUploadUrlResponse(
        Integer imageId,
        @JsonProperty("presignedURL")
        @Schema(description = "MinIO/S3에 직접 PUT할 URL")
        String presignedUrl,
        @Schema(description = "Presigned URL 유효시간(초)", example = "600")
        long uploadExpiresIn
) {
}
