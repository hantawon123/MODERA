package com.ssafy.modera.worker.domain.analysis.callback.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/*
 * AI 서버 (FastAPI)가 분석 완료 후 보내는 콜백 바디.
 * result는 statge마다 필드가 달라 고정 타입으로 못 받는다(AI 쪽은 자유 dict)
 *
 * 검증은 "없으면 서버가 터지는 값"에만 건다.
 *  - jobId  : findById 인자. null이면 NPE
 *  - status : switch 대상. null이면 NPE
 *  - imageId: 결과 저장 시 NOT NULL 컬럼
 * status 값 자체(COMPLETED/FAILED/EMPTY)는 @Pattern으로 막지 않는다. 모르는 값이
 * 오면 AnalysisCallbackService가 경고만 남기고 200으로 받아넘기는데, 400을 주면
 * AI가 3회 재시도(spring_client.CALLBACK_MAX_ATTEMPTS)를 헛돌기 때문이다.
 * 새 status가 추가돼도 재시도 폭풍 없이 로그로만 드러나는 편이 낫다.
 */
public record AnalysisCallbackRequest(
        @NotNull Integer jobId,
        @NotNull Integer imageId,
        String stage,
        @NotBlank String status,        // COMPLETED | FAILED | EMPTY
        Map<String, Object> result,     // COMPLETED일 때만 채워짐
        CallbackError error,            // FAILED일 때만 채워짐
        String modelVersion,
        String completedAt
) {
    public record CallbackError(
            String code,
            String message,
            Boolean retryable
    ) {}
}
