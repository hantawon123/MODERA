package com.ssafy.modera.contract;

/**
 * EventEnvelope.eventType 값으로 쓰이는 이벤트 종류 상수.
 */
public final class EventTypes {

    public static final String IMAGE_UPLOADED = "IMAGE_UPLOADED";
    public static final String ANALYSIS_COMPLETED = "ANALYSIS_COMPLETED";
    public static final String ANALYSIS_FAILED = "ANALYSIS_FAILED";

    private EventTypes() {
    }
}
