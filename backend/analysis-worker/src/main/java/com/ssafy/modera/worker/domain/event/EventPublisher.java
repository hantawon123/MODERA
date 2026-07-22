package com.ssafy.modera.worker.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.contract.EventEnvelope;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class EventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String streamKey, String eventType, int version, Object payload) {
        EventEnvelope envelope = EventEnvelope.of(eventType, version, Instant.now().toString(), payload, objectMapper);
        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .ofMap(envelope.toFieldMap())
                .withStreamKey(streamKey);
        redisTemplate.opsForStream().add(record);
    }
}
