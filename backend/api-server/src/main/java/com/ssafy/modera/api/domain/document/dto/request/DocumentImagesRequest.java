package com.ssafy.modera.api.domain.document.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * 문서 구성 이미지 추가(8-6)·제외(8-7) 요청.
 *
 * <p>재분석({@link DocumentRegenerateRequest})과 필드는 같지만 imageIds의 의미가 다르다 —
 * 이쪽은 <b>바꾸려는 이미지</b>이고, 재분석은 <b>최종 목록</b>이다. 그래서 DTO를 나눴다.
 *
 * <p>"무엇을 바꿀지"로 받는 이유: 앱이 들고 있는 구성 목록은 낡을 수 있다(다른 기기에서
 * 이미지를 지웠다든지). 최종 목록으로 받으면 그 낡은 목록이 그대로 문서 구성이 되지만,
 * 변경분으로 받으면 서버가 그 시점의 실제 구성을 기준으로 계산한다.
 */
public record DocumentImagesRequest(
        @Schema(description = "중복 요청 방지용 클라이언트 요청 ID", example = "9e41d582-471f-47b1-b50b-d56d2a97ed92")
        @NotNull(message = "clientRequestId는 필수입니다.")
        UUID clientRequestId,

        @Schema(description = "추가하거나 제외할 본인 소유 이미지 ID")
        @NotEmpty(message = "imageIds는 1개 이상이어야 합니다.")
        List<Integer> imageIds
) {
}
