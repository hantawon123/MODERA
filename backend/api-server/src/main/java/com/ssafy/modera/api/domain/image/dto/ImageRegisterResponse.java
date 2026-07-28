package com.ssafy.modera.api.domain.image.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ImageRegisterResponse(
        List<Registered> registered,
        List<Duplicated> duplicated,
        List<Failed> failed
) {
    public record Registered(
            Integer imageId,
            String fileName,

            @JsonProperty("presignedURL")
            @Schema(description = "MinIO/S3에 직접 PUT할 URL")
            String presignedUrl,

            @Schema(description = "presigned URL 유효시간(초)", example = "600")
            long uploadExpiresIn
    ) {
    }

    public record Duplicated(
            String fileName,
            Integer existingImageId
    ) {
    }

    public record Failed(
            String fileName,
            String reason
    ) {
    }
}
