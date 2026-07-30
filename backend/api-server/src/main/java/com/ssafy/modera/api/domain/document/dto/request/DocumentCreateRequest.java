package com.ssafy.modera.api.domain.document.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * 8-2 문서 생성 요청.
 *
 * <p>중복·개수 상한은 서비스에서 검증한다 — 애너테이션으로 표현하면 400 응답의 필드 오류
 * 메시지가 "왜 안 되는지"를 설명하지 못하고, 상한값이 AI 서버 제약(MAX_IMAGES)에서 오는
 * 값이라 서비스 쪽에 두는 편이 근거가 드러난다.
 */
public record DocumentCreateRequest(
        @Schema(description = "중복 요청 방지용 클라이언트 요청 ID", example = "d95db8b7-897e-412c-8924-eef3c7bca039")
        @NotNull(message = "clientRequestId는 필수입니다.")
        UUID clientRequestId,

        @Schema(description = "문서로 만들 본인 소유 이미지 ID. 순서가 유지되며 첫 번째가 중심 자료로 쓰인다.")
        @NotEmpty(message = "imageIds는 1개 이상이어야 합니다.")
        List<Integer> imageIds
) {
}
