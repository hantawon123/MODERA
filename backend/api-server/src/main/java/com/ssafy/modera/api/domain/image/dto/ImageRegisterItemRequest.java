package com.ssafy.modera.api.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record ImageRegisterItemRequest(
        @Schema(example = "d95db8b7-897e-412c-8924-eef3c7bca039")
        @NotBlank String clientRequestId,

        @Schema(example = "60.jpg")
        @NotBlank String fileName,

        @Schema(description = "SHA-256 해시(64자리 hex)")
        @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F]{64}$")
        String contentHash,

        @Schema(description = "파일 크기(bytes)", example = "546543")
        @NotNull @Positive Integer fileSize,

        @Schema(description = "클라이언트에서 수행한 OCR 결과")
        @NotNull @Valid ImageRegisterOcrRequest ocr
) {
}
