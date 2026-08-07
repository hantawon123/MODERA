package com.ssafy.modera.api.domain.document.event;

import com.ssafy.modera.api.domain.document.entity.DocumentGenerationRequest;
import com.ssafy.modera.api.domain.document.repository.DocumentGenerationRequestRepository;
import com.ssafy.modera.api.domain.document.service.DocumentGenerationResult;
import com.ssafy.modera.api.domain.document.service.DocumentPersistService;
import com.ssafy.modera.contract.payload.DocumentCompletedPayload;
import com.ssafy.modera.contract.payload.DocumentFailedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * 문서 생성 결과 이벤트 처리.
 *
 * <p><b>지금은 쓰이지 않는 경로다</b> — 문서 생성이 api-server의 동기 호출로 바뀌면서
 * api는 더 이상 DOCUMENT_REQUESTED를 발행하지 않는다. 그래도 남겨 두는 이유는 두
 * 가지다. 첫째, 배포 시점 차이로 아직 스트림에 남아 있던 요청의 결과가 뒤늦게 도착할
 * 수 있다. 둘째, 이벤트 경유 방식으로 되돌릴 여지를 남긴다(worker 쪽 경로도 그대로다).
 *
 * <p>저장 자체는 동기 경로와 같은 {@link DocumentPersistService}를 쓴다 — 두 벌로
 * 갈라지면 한쪽만 고치는 사고가 난다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentResultEventHandler {

    private final DocumentGenerationRequestRepository documentGenerationRequestRepository;
    private final DocumentPersistService documentPersistService;

    @Transactional
    public void handleCompleted(DocumentCompletedPayload payload) {
        DocumentGenerationRequest request = findClaimable(payload.documentRequestId());
        if (request == null) {
            return;
        }

        documentPersistService.persist(request.getId(), new DocumentGenerationResult(
                payload.title(),
                payload.summary(),
                payload.markdown(),
                payload.sourceImageIds()
        ));
    }

    @Transactional
    public void handleFailed(DocumentFailedPayload payload) {
        DocumentGenerationRequest request = findClaimable(payload.documentRequestId());
        if (request == null) {
            return;
        }

        request.fail(payload.errorCode(), OffsetDateTime.now());
        log.warn("문서 생성 실패 기록: documentRequestId={} code={} retryable={}",
                request.getId(), payload.errorCode(), payload.retryable());
    }

    /**
     * 처리 대상 요청을 잠근 채로 가져온다. 처리하면 안 되는 경우는 전부 null이다.
     *
     * <p>QUEUED가 아니면 이미 확정된 요청이라 건너뛴다 — 같은 요청의 완료 이벤트가 서로 다른
     * eventId로 두 번 도착할 수 있어서(worker의 PEL 재처리), 이 체크가 없으면 문서가 두 개
     * 생긴다. 예외를 던지지 않는 이유는 중복 수신이 at-least-once 환경의 정상 상황이기
     * 때문이다 — 던지면 컨슈머가 일시 오류로 보고 XACK을 미뤄 무한히 재전달된다.
     */
    private DocumentGenerationRequest findClaimable(String documentRequestId) {
        Integer id = parseId(documentRequestId);
        if (id == null) {
            return null;
        }

        DocumentGenerationRequest request = documentGenerationRequestRepository
                .findByIdForUpdate(id).orElse(null);
        if (request == null) {
            log.warn("문서 생성 요청을 찾지 못해 무시한다: documentRequestId={}", documentRequestId);
            return null;
        }
        if (!request.isQueued()) {
            log.info("이미 확정된 요청이라 무시한다: documentRequestId={} status={}",
                    documentRequestId, request.getStatus());
            return null;
        }
        return request;
    }

    /** documentRequestId는 api가 만든 PK 문자열이다. 숫자가 아니면 우리 이벤트가 아니다. */
    private Integer parseId(String documentRequestId) {
        try {
            return Integer.valueOf(documentRequestId);
        } catch (NumberFormatException | NullPointerException exception) {
            log.error("documentRequestId 형식이 올바르지 않아 무시한다: value={}", documentRequestId);
            return null;
        }
    }
}
