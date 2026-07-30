package com.ssafy.modera.api.domain.image.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "image_registration_request", schema = "image_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageRegistrationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_registration_request_id")
    private Integer id;
    @Column(name = "user_id", nullable = false)
    private Integer userId;
    @Column(name = "client_request_id", nullable = false)
    private UUID clientRequestId;
    @Column(name = "image_id")
    private Integer imageId;
    @Column(name = "status", nullable = false)
    private String status = "PROCESSING";
    @Column(name = "failure_reason")
    private String failureReason;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "del_yn", nullable = false, length = 1)
    private String delYn = "N";

    public ImageRegistrationRequest(Integer userId, UUID clientRequestId, OffsetDateTime now) {
        this.userId = userId;
        this.clientRequestId = clientRequestId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void complete(Integer imageId, boolean duplicated, OffsetDateTime now) {
        this.imageId = imageId;
        this.status = duplicated ? "DUPLICATED" : "REGISTERED";
        this.failureReason = null;
        this.updatedAt = now;
        this.completedAt = now;
    }
}
