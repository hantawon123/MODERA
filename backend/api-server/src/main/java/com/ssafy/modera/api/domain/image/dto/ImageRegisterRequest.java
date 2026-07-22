package com.ssafy.modera.api.domain.image.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImageRegisterRequest(
        @NotBlank String clientRequestId,
        @NotBlank String fileName,
        String contentType,
        @NotBlank String contentHash,
        @NotNull Integer fileSize
) {
}
