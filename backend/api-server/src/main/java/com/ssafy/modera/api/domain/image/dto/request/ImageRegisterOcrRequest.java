package com.ssafy.modera.api.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageRegisterOcrRequest(
        @Schema(description = "클라이언트 OCR 원문")
        String rawText
) {
}
