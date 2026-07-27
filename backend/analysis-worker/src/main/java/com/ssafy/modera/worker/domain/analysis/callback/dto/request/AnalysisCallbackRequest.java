package com.ssafy.modera.worker.domain.analysis.callback.dto.request;

import java.util.Map;

/*
 * AI 서버 (FastAPI)가 분석 완료 후 보내는 콜백 바디.
 * result는 statge마다 필드가 달라 고정 타입으로 못 받는다(AI 쪽은 자유 dict)
 */
public record AnalysisCallbackRequest(
        Integer jobId,
        Integer imageId,
        String stage,
        String status,                  // COMPLETED | FAILED | EMPTY
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
