package com.ssafy.modera.api.domain.document.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * 문서 재분석 요청.
 *
 * <p>이미지를 추가하든 제외하든 그대로 두든 전부 이 요청 하나로 처리한다 — 서버가 받는
 * 것은 언제나 "이 문서를 이 최종 이미지 목록으로 다시 만들어라"라서, 무엇이 추가되고
 * 무엇이 빠졌는지 서버가 알 필요가 없다.
 */
public record DocumentRegenerateRequest(
        @Schema(description = "중복 요청 방지용 클라이언트 요청 ID", example = "9e41d582-471f-47b1-b50b-d56d2a97ed92")
        @NotNull(message = "clientRequestId는 필수입니다.")
        UUID clientRequestId,

        @Schema(description = """
                재분석에 쓸 최종 이미지 ID 목록. 생략하거나 비워 보내면 현재 문서를 구성하는
                이미지를 그대로 쓴다(내용만 다시 정리하는 재분석).
                """)
        List<Integer> imageIds
) {
}
