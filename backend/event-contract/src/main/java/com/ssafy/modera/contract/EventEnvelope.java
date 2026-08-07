package com.ssafy.modera.contract;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Redis Streams에 실리는 모든 이벤트의 공통 envelope.
 * payload는 JSON 문자열로 직렬화해 payloadJson에 담는다(스트림 필드 맵은 문자열만 허용).
 */
public record EventEnvelope(
        String eventId,
        String eventType,
        int version,
        String occurredAt,
        String payloadJson
) {

    private static final String FIELD_EVENT_ID = "eventId";
    private static final String FIELD_EVENT_TYPE = "eventType";
    private static final String FIELD_VERSION = "version";
    private static final String FIELD_OCCURRED_AT = "occurredAt";
    private static final String FIELD_PAYLOAD = "payload";

    /**
     * payload 객체를 JSON으로 직렬화하고 eventId를 새로 발급해 envelope을 만든다.
     */
    public static EventEnvelope of(String eventType, int version, String occurredAt,
                                    Object payload, ObjectMapper objectMapper) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            return new EventEnvelope(UUID.randomUUID().toString(), eventType, version, occurredAt, payloadJson);
        } catch (Exception e) {
            throw new IllegalStateException("이벤트 payload 직렬화 실패: eventType=" + eventType, e);
        }
    }

    /**
     * XADD로 보낼 Redis Streams 필드 맵으로 변환한다.
     */
    public Map<String, String> toFieldMap() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(FIELD_EVENT_ID, eventId);
        fields.put(FIELD_EVENT_TYPE, eventType);
        fields.put(FIELD_VERSION, String.valueOf(version));
        fields.put(FIELD_OCCURRED_AT, occurredAt);
        fields.put(FIELD_PAYLOAD, payloadJson);
        return fields;
    }

    /**
     * XREADGROUP으로 받은 Redis Streams 필드 맵에서 envelope을 복원한다.
     */
    public static EventEnvelope fromFieldMap(Map<String, String> fields) {
        return new EventEnvelope(
                fields.get(FIELD_EVENT_ID),
                fields.get(FIELD_EVENT_TYPE),
                Integer.parseInt(fields.get(FIELD_VERSION)),
                fields.get(FIELD_OCCURRED_AT),
                fields.get(FIELD_PAYLOAD)
        );
    }

    /**
     * payloadJson을 지정한 타입으로 역직렬화한다.
     */
    public <T> T readPayload(Class<T> payloadType, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(payloadJson, payloadType);
        } catch (Exception e) {
            throw new IllegalStateException("이벤트 payload 역직렬화 실패: eventType=" + eventType, e);
        }
    }
}
