package com.ssafy.modera.api.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ImageRegisterOcrRequest(
        @Schema(description = "클라이언트 OCR 원문")
        @NotBlank String rawText
) {
}
