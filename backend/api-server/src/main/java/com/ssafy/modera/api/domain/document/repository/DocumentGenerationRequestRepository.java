package com.ssafy.modera.api.domain.document.repository;

import com.ssafy.modera.api.domain.document.entity.DocumentGenerationRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentGenerationRequestRepository extends JpaRepository<DocumentGenerationRequest, Integer> {

    /**
     * 완료·실패 이벤트 처리용 조회. 행을 잠근 채로 읽는다.
     *
     * <p>같은 요청의 DOCUMENT_COMPLETED가 두 번 도착할 수 있다 — 컨슈머의 eventId dedup은
     * 같은 이벤트의 재전달만 막고, worker가 PEL 재처리로 AI를 두 번 불러 서로 다른 eventId로
     * 발행하는 경우는 막지 못한다(DocumentCompletedPayload javadoc 참고). 잠그지 않으면 두
     * 트랜잭션이 나란히 QUEUED를 읽고 문서를 두 개 만든다. 뒤에 온 쪽은 잠금이 풀린 뒤
     * COMPLETED를 보고 그냥 빠진다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM DocumentGenerationRequest r WHERE r.id = :id")
    Optional<DocumentGenerationRequest> findByIdForUpdate(@Param("id") Integer id);

    /**
     * 8-2의 DUPLICATE_CLIENT_REQUEST 판정용. UNIQUE 제약이 최종 방어선이고 이 조회는
     * 정상 경로에서 409를 돌려주기 위한 것이다(제약 위반 예외를 409로 번역하는 것보다
     * 의도가 드러난다).
     */
    Optional<DocumentGenerationRequest> findByUserIdAndClientRequestIdAndDelYn(
            Integer userId, UUID clientRequestId, String delYn);

    /**
     * 재분석 진행 중 판정용. 접수 시에는 409로 끊고, 상세 조회(8-3)에서는 로딩 표시로 쓴다.
     *
     * <p>동시 요청은 이 조회만으로 못 막는다 — 최종 방어선은 036의 부분 유니크 인덱스
     * (uq_document_generation_request_inflight)다.
     */
    boolean existsByUserIdAndSourceDocumentIdAndStatusAndDelYn(
            Integer userId, Integer sourceDocumentId, String status, String delYn);
}
