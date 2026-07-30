package com.ssafy.modera.contract.payload;

import java.util.List;

/**
 * {@link com.ssafy.modera.contract.EventTypes#DOCUMENT_COMPLETED} 이벤트 payload.
 * analysis-worker가 발행하고 api-server가 소비한다(스트림: analysis-result).
 * api-server는 documentRequestId 기준으로 멱등하게 document_schema에 저장한다 —
 * PEL 재처리로 같은 요청의 COMPLETED가 서로 다른 eventId로 두 번 도착할 수 있다.
 *
 * @param markdown 생성된 문서 본문. 수십 KB까지 갈 수 있지만 문서 이벤트는 드물어
 *                 스트림에 그대로 싣는다(별도 저장소를 두는 비용이 더 크다).
 */
public record DocumentCompletedPayload(
        String documentRequestId,
        int userId,
        String title,
        String markdown,
        List<Integer> sourceImageIds,
        String modelVersion,
        String generatedAt
) {
}
