package com.ssafy.modera.api.domain.image.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record ImageRegisterResponse(
        List<Registered> registered,
        List<Duplicated> duplicated,
        List<Failed> failed
) {
    public record Registered(
            UUID clientRequestId,
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
            UUID clientRequestId,
            String fileName,
            Integer existingImageId
    ) {
    }

    public record Failed(
            UUID clientRequestId,
            String fileName,
            String reason
    ) {
    }
}
