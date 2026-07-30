package com.ssafy.modera.api.domain.document.entity;

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

/**
 * 문서 생성 요청 이력이자 멱등키 보관소.
 *
 * <p>이 행의 PK가 곧 이벤트의 {@code documentRequestId}다. client_request_id를 쓰지 않는
 * 이유는 그 값이 {@code (user_id, client_request_id)}로만 유일해서, 완료 이벤트를 받았을 때
 * 어느 요청인지 단독으로 특정할 수 없기 때문이다. PK는 전역 유일이라 매칭이 단순하다.
 * 클라이언트에게 돌려주는 clientRequestId는 요청 값을 그대로 echo하면 된다.
 *
 * <p>상태는 {@code QUEUED → COMPLETED | FAILED}로만 움직인다. CHECK에 PROCESSING도
 * 있지만 worker가 api DB에 접속하지 않아 "처리 시작"을 알릴 방법이 없어 쓰이지 않는다.
 */
@Entity
@Table(name = "document_generation_request", schema = "document_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentGenerationRequest {

    public static final String OPERATION_CREATE = "CREATE";

    /** 재분석. 새 문서를 만들지 않고 source_document_id가 가리키는 문서를 갱신한다. */
    public static final String OPERATION_REGENERATE = "REGENERATE";

    public static final String STATUS_QUEUED = "QUEUED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    /** failure_reason은 VARCHAR(100)이라 이벤트 메시지를 그대로 넣으면 넘칠 수 있다. */
    private static final int MAX_FAILURE_REASON = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_generation_request_id")
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "client_request_id", nullable = false)
    private UUID clientRequestId;

    @Column(name = "operation_type", nullable = false, length = 20)
    private String operationType;

    @Column(name = "source_document_id")
    private Integer sourceDocumentId;

    @Column(name = "result_document_id")
    private Integer resultDocumentId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_QUEUED;

    @Column(name = "failure_reason", length = MAX_FAILURE_REASON)
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

    /** 8-2 문서 생성. 재분석은 source_document_id가 필요해 별도 팩토리로 둔다. */
    public static DocumentGenerationRequest create(Integer userId, UUID clientRequestId, OffsetDateTime now) {
        DocumentGenerationRequest request = new DocumentGenerationRequest();
        request.userId = userId;
        request.clientRequestId = clientRequestId;
        request.operationType = OPERATION_CREATE;
        request.status = STATUS_QUEUED;
        request.createdAt = now;
        request.updatedAt = now;
        return request;
    }

    /**
     * 재분석 요청. 이미지 목록을 그대로 두고 다시 만들든, 추가·제외해서 다시 만들든
     * 전부 이 하나로 처리한다 — 서버 입장에서는 "이 문서를 이 최종 이미지 목록으로 다시
     * 만들어라"가 전부라, 무엇이 추가·제외됐는지 구분할 이유가 없다.
     */
    public static DocumentGenerationRequest regenerate(
            Integer userId, UUID clientRequestId, Integer sourceDocumentId, OffsetDateTime now) {
        DocumentGenerationRequest request = new DocumentGenerationRequest();
        request.userId = userId;
        request.clientRequestId = clientRequestId;
        request.operationType = OPERATION_REGENERATE;
        request.sourceDocumentId = sourceDocumentId;
        request.status = STATUS_QUEUED;
        request.createdAt = now;
        request.updatedAt = now;
        return request;
    }

    /** 재분석이면 새 문서를 만들지 않고 sourceDocumentId를 갱신해야 한다. */
    public boolean isRegeneration() {
        return sourceDocumentId != null;
    }

    public void complete(Integer documentId, OffsetDateTime now) {
        this.resultDocumentId = documentId;
        this.status = STATUS_COMPLETED;
        this.failureReason = null;
        this.updatedAt = now;
        this.completedAt = now;
    }

    public void fail(String reason, OffsetDateTime now) {
        this.status = STATUS_FAILED;
        this.failureReason = truncateReason(reason);
        this.updatedAt = now;
        this.completedAt = now;
    }

    public boolean isQueued() {
        return STATUS_QUEUED.equals(status);
    }

    private static String truncateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown";
        }
        return reason.length() <= MAX_FAILURE_REASON ? reason : reason.substring(0, MAX_FAILURE_REASON);
    }
}
