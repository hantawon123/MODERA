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

@Entity
@Table(name = "analysis_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id")
    private Integer jobId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "image_id", nullable = false)
    private Integer imageId;

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

    // 원본 객체 키. 재시도(AnalysisRetryScanner)가 AI에 다시 요청할 때 필요한데,
    // worker는 modera_api를 조회할 수 없으므로 IMAGE_UPLOADED payload 값을 job에
    // 실어 나른다(client_ocr_*와 같은 이유). 008 changeset 이전 행은 NULL이다.
    @Column(name = "s3_key")
    private String s3Key;

    // 클라이언트가 업로드 시점에 수행한 온디바이스 OCR. IMAGE_UPLOADED로 들어오지만
    // 이 값을 쓰는 결과 저장은 한참 뒤 AI 콜백에서 일어나므로 job 행에 실어 나른다.
    // AI 콜백은 refined 텍스트만 돌려주고 raw/lang/confidence는 echo하지 않는다.
    @Column(name = "client_ocr_raw_text")
    private String clientOcrRawText;

    @Column(name = "client_ocr_lang")
    private String clientOcrLang;

    @Column(name = "client_ocr_confidence")
    private Float clientOcrConfidence;

    @Builder
    public AnalysisJob(Integer userId, Integer imageId, String stage, String status, Integer attempt,
                        String triggerType, OffsetDateTime queuedAt, String s3Key,
                        String clientOcrRawText, String clientOcrLang, Float clientOcrConfidence) {
        this.userId = userId;
        this.imageId = imageId;
        this.stage = stage;
        this.status = status;
        this.attempt = attempt;
        this.triggerType = triggerType;
        this.queuedAt = queuedAt;
        this.s3Key = s3Key;
        this.clientOcrRawText = clientOcrRawText;
        this.clientOcrLang = clientOcrLang;
        this.clientOcrConfidence = clientOcrConfidence;
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
