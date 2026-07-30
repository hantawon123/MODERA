package com.ssafy.modera.api.domain.document.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** 8-2의 202 응답. 실제 문서는 비동기로 만들어지므로 접수 사실만 돌려준다. */
public record DocumentGenerationAcceptedResponse(
        @Schema(description = "요청한 clientRequestId를 그대로 돌려준다") UUID clientRequestId,
        @Schema(description = "접수 시점 상태", example = "QUEUED") String status
) {
}
