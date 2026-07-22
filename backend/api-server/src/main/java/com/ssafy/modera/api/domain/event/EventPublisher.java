package com.ssafy.modera.api.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.contract.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String streamKey, String eventType, int version, Object payload) {
        EventEnvelope envelope = EventEnvelope.of(eventType, version, Instant.now().toString(), payload, objectMapper);
        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .ofMap(envelope.toFieldMap())
                .withStreamKey(streamKey);
        redisTemplate.opsForStream().add(record);
        log.info("이벤트 발행: eventId={}, eventType={}, stream={}", envelope.eventId(), envelope.eventType(), streamKey);
    }
}
