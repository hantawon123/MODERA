package com.ssafy.modera.api.domain.storage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * MinIO ObjectCreated 버킷 알림 webhook payload 중 필요한 필드만 매핑한다.
 * MinIO는 AWS S3 이벤트 알림과 동일한 스키마(Records[].s3.bucket.name / object.key)를 쓴다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MinioWebhookEvent(@JsonProperty("Records") List<Record> records) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Record(S3 s3) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3(Bucket bucket, S3Object object) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Bucket(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record S3Object(String key) {
    }
}
