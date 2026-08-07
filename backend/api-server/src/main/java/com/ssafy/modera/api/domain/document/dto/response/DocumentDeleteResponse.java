package com.ssafy.modera.api.domain.document.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 8-5 문서 삭제 결과. 재료였던 이미지는 지워지지 않는다. */
public record DocumentDeleteResponse(
        @Schema(description = "삭제한 문서 ID", example = "101") Integer documentId,
        @Schema(description = "항상 true. 이미 삭제된 문서는 404다", example = "true") boolean deleted
) {
}
