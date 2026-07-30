package com.ssafy.modera.contract.payload;

/**
 * {@link com.ssafy.modera.contract.EventTypes#DOCUMENT_FAILED} 이벤트 payload.
 * analysis-worker가 발행하고 api-server가 소비한다(스트림: analysis-result).
 *
 * <p>worker는 문서 생성을 자동 재시도하지 않는다(재시도 = LLM 비용, 사용자가 버튼을
 * 다시 누르는 게 재시도다). retryable은 "다시 눌러볼 가치가 있는가"를 뜻한다 —
 * 5xx·타임아웃은 true, 요청 자체가 거부된 4xx는 false.
 */
public record DocumentFailedPayload(
        String documentRequestId,
        int userId,
        String errorCode,
        String errorMessage,
        boolean retryable
) {
}
