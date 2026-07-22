package com.ssafy.modera.api.domain.image.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ImageRegisterRequest(
        @Schema(description = "클라이언트가 생성하는 요청 식별자. 같은 값으로 재요청하면 새로 만들지 " +
                "않고 기존 이미지에 대한 응답을 돌려준다(멱등 키)", example = "local-generated-uuid-001")
        @NotBlank String clientRequestId,

        @Schema(description = "원본 파일명", example = "photo.png")
        @NotBlank String fileName,

        @Schema(description = "MIME 타입(선택)", example = "image/png")
        String contentType,

        @Schema(description = "파일 SHA-256 해시, 64자 hex", example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85")
        @NotBlank String contentHash,

        @Schema(description = "파일 크기(bytes)", example = "384211")
        @NotNull Integer fileSize
) {
}
