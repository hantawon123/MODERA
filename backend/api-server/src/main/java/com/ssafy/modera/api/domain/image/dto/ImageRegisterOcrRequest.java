package com.ssafy.modera.api.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImageRegisterOcrRequest(
        @Schema(description = "클라이언트 OCR 원문")
        @NotBlank String rawText,

        @Schema(description = "OCR 언어 코드", example = "ko")
        @NotBlank @Size(max = 10) String lang
) {
}
