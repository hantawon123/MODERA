package com.ssafy.modera.api.domain.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * library_schema.user_image. user_id/image_id는 각각 user_schema/image_schema가
 * 소유한 값의 논리 참조(같은 DB 안이지만 schema 경계를 넘는 FK는 만들지 않는다).
 * category_id만 같은 library_schema 내부 FK라 컬럼으로 그대로 들고 있는다(연관관계 매핑 없음).
 */
@Entity
@Table(name = "user_image", schema = "library_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserImage {

    @Id
    @Column(name = "image_id")
    private UUID imageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "client_request_id", nullable = false)
    private String clientRequestId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "title")
    private String title;

    @Column(name = "favorite", nullable = false)
    private Boolean favorite;

    @Column(name = "analysis_status", nullable = false)
    private String analysisStatus;

    @Column(name = "last_viewed_at")
    private OffsetDateTime lastViewedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public UserImage(UUID imageId, Long userId, String clientRequestId, String title,
                      OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.imageId = imageId;
        this.userId = userId;
        this.clientRequestId = clientRequestId;
        this.title = title;
        this.favorite = false;
        this.analysisStatus = "NONE";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void applyAnalysisStatus(String analysisStatus, OffsetDateTime updatedAt) {
        this.analysisStatus = analysisStatus;
        this.updatedAt = updatedAt;
    }
}
