package com.ssafy.modera.contract;

/**
 * EventEnvelope.eventType 값으로 쓰이는 이벤트 종류 상수.
 */
public final class EventTypes {

    public static final String IMAGE_UPLOADED = "IMAGE_UPLOADED";
    public static final String ANALYSIS_COMPLETED = "ANALYSIS_COMPLETED";
    public static final String ANALYSIS_FAILED = "ANALYSIS_FAILED";

    // 문서 생성(마크다운). 전용 스트림을 만들지 않고 기존 스트림에 실어 보낸다 —
    // DOCUMENT_REQUESTED는 image-analysis(api→worker), COMPLETED/FAILED는
    // analysis-result(worker→api)로 방향이 기존 이벤트와 같고, 같은 스트림이면
    // 컨슈머 루프·PEL 회수(PelReclaimScanner)가 그대로 적용된다. 스트림을 새로 파면
    // 그 인프라를 통째로 복제해야 한다.
    public static final String DOCUMENT_REQUESTED = "DOCUMENT_REQUESTED";
    public static final String DOCUMENT_COMPLETED = "DOCUMENT_COMPLETED";
    public static final String DOCUMENT_FAILED = "DOCUMENT_FAILED";

    public static final String IMAGE_SEMANTIC_SEARCH_REQUESTED =
            "IMAGE_SEMANTIC_SEARCH_REQUESTED";
    public static final String IMAGE_SEARCH_COMPLETED = "IMAGE_SEARCH_COMPLETED";
    public static final String IMAGE_SEARCH_FAILED = "IMAGE_SEARCH_FAILED";

    private EventTypes() {
    }
}
