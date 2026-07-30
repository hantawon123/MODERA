package com.ssafy.modera.contract.payload;

import java.util.List;

/**
 * {@link com.ssafy.modera.contract.EventTypes#DOCUMENT_COMPLETED} 이벤트 payload.
 * analysis-worker가 발행하고 api-server가 소비한다(스트림: analysis-result).
 * api-server는 documentRequestId 기준으로 멱등하게 document_schema에 저장한다 —
 * PEL 재처리로 같은 요청의 COMPLETED가 서로 다른 eventId로 두 번 도착할 수 있다.
 *
 * @param summary  AI가 채워 보내는 문서 요약. 본문을 열지 않고 목록에서 보여줄 값이다.
 * @param markdown 생성된 문서 본문. 수십 KB까지 갈 수 있지만 문서 이벤트는 드물어
 *                 스트림에 그대로 싣는다(별도 저장소를 두는 비용이 더 크다).
 * @param sections 구조화된 본문. markdown과 같은 내용을 단락 단위로 쪼갠 것이라 문서
 *                 원본은 markdown 하나로 충분하고, 이쪽은 화면 구성과 이미지 연결에 쓴다.
 *                 AI가 sections를 주지 않아도 markdown만 있으면 정상 완료이므로 빈 리스트일 수 있다.
 */
public record DocumentCompletedPayload(
        String documentRequestId,
        int userId,
        String title,
        String summary,
        String markdown,
        List<Section> sections,
        List<Integer> sourceImageIds,
        String modelVersion,
        String generatedAt
) {

    /**
     * 문서 본문의 한 단락.
     *
     * <p>필드명은 API 명세 6-3의 contents에 맞췄다(sequence/contentTitle/contentText).
     * worker는 이름만 바꾸고 내용은 가공하지 않는다 — AI 스펙 변경 시 이 매핑만 고치면
     * 되는 어댑터 지점이다.
     *
     * @param sequence     배열 순서 1부터. AI는 주지 않으므로 worker가 부여한다.
     * @param contentTitle AI 응답의 heading.
     * @param contentText  AI 응답의 body 그대로. bullets를 여기 합치지 않는다 — 구조를
     *                     보존해야 클라이언트가 문단과 목록을 구분해 렌더링할 수 있다.
     * @param bullets      AI 응답의 bullets 그대로. 없으면 빈 리스트.
     * @param imageIds     AI 응답의 imageIds 그대로. 6-3 명세에는 없지만 api-server가
     *                     image_document 연결 테이블을 채우는 데 쓴다. 없으면 빈 리스트.
     */
    public record Section(
            int sequence,
            String contentTitle,
            String contentText,
            List<String> bullets,
            List<Integer> imageIds
    ) {
    }
}
