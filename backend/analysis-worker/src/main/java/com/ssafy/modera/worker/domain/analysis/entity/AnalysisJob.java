package com.ssafy.modera.worker.domain.analysis.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analysis_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "image_id", nullable = false)
    private UUID imageId;

    @Column(name = "stage", nullable = false)
    private String stage;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "attempt", nullable = false)
    private Integer attempt;

    @Column(name = "trigger_type")
    private String triggerType;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retryable")
    private Boolean retryable;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "queued_at")
    private OffsetDateTime queuedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Builder
    public AnalysisJob(UUID imageId, String stage, String status, Integer attempt,
                        String triggerType, OffsetDateTime queuedAt) {
        this.imageId = imageId;
        this.stage = stage;
        this.status = status;
        this.attempt = attempt;
        this.triggerType = triggerType;
        this.queuedAt = queuedAt;
    }

    public void markProcessing(OffsetDateTime startedAt) {
        this.status = "PROCESSING";
        this.startedAt = startedAt;
    }

    public void markCompleted(String modelVersion, OffsetDateTime completedAt) {
        this.status = "COMPLETED";
        this.modelVersion = modelVersion;
        this.completedAt = completedAt;
    }

    public void markFailed(String errorCode, String errorMessage, boolean retryable, OffsetDateTime completedAt) {
        this.status = "FAILED";
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.retryable = retryable;
        this.completedAt = completedAt;
    }
}
