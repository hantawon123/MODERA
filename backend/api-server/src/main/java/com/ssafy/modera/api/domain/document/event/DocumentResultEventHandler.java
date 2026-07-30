package com.ssafy.modera.api.domain.document.event;

import com.ssafy.modera.api.domain.document.entity.Document;
import com.ssafy.modera.api.domain.document.entity.DocumentGenerationRequest;
import com.ssafy.modera.api.domain.document.repository.DocumentCommandRepository;
import com.ssafy.modera.api.domain.document.repository.DocumentGenerationRequestRepository;
import com.ssafy.modera.api.domain.document.repository.DocumentRepository;
import com.ssafy.modera.api.domain.document.repository.DocumentViewRepository;
import com.ssafy.modera.api.domain.library.entity.ImageDocument;
import com.ssafy.modera.api.domain.library.entity.UserDocument;
import com.ssafy.modera.api.domain.library.repository.ImageDocumentRepository;
import com.ssafy.modera.api.domain.library.repository.UserDocumentRepository;
import com.ssafy.modera.contract.payload.DocumentCompletedPayload;
import com.ssafy.modera.contract.payload.DocumentFailedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 문서 생성 결과 이벤트 처리(8-2의 7번 단계).
 *
 * <p>원본·관계·조회 모델을 하나의 트랜잭션으로 저장한다. 하나라도 실패하면 전부 롤백되고
 * 요청은 QUEUED로 남아, 컨슈머의 일시 오류 정책(XACK 보류 → PEL 재전달)으로 다시 시도된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentResultEventHandler {

    private final DocumentGenerationRequestRepository documentGenerationRequestRepository;
    private final DocumentRepository documentRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final ImageDocumentRepository imageDocumentRepository;
    private final DocumentViewRepository documentViewRepository;
    private final DocumentCommandRepository documentCommandRepository;

    @Transactional
    public void handleCompleted(DocumentCompletedPayload payload) {
        DocumentGenerationRequest request = findClaimable(payload.documentRequestId());
        if (request == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        // AI가 내용 없는 이미지를 건너뛰면 요청보다 적을 수 있다. 요청 목록이 아니라
        // 실제로 문서에 쓰인 목록으로 관계를 만든다.
        List<Integer> imageIds = payload.sourceImageIds() == null ? List.of() : payload.sourceImageIds();

        if (request.isRegeneration()) {
            applyRegeneration(request, payload, imageIds, now);
            return;
        }
        createDocument(request, payload, imageIds, now);
    }

    private void createDocument(DocumentGenerationRequest request, DocumentCompletedPayload payload,
                                List<Integer> imageIds, OffsetDateTime now) {
        Document document = documentRepository.save(
                new Document(payload.title(), payload.summary(), payload.markdown(), now));
        userDocumentRepository.save(UserDocument.builder()
                .userId(request.getUserId())
                .documentId(document.getDocumentId())
                .build());

        for (Integer imageId : imageIds) {
            ImageDocument relation = imageDocumentRepository.save(ImageDocument.builder()
                    .imageId(imageId)
                    .documentId(document.getDocumentId())
                    .updatedAt(now)
                    .build());
            boolean copied = documentViewRepository.insertDocumentImageView(
                    relation.getImageDocumentId(), request.getUserId(),
                    document.getDocumentId(), imageId, now);
            if (!copied) {
                // 접수 때 검증한 이미지라 정상 경로에서는 있어야 한다. 없다는 건 그 사이
                // 삭제됐거나, AI가 요청하지 않은 imageId를 돌려줬다는 뜻이다. 관계는 남기고
                // 조회 모델만 비는 상태가 되므로 조용히 넘기지 않고 흔적을 남긴다.
                log.warn("조회 모델에 복사할 이미지를 찾지 못했다: documentId={} imageId={}",
                        document.getDocumentId(), imageId);
            }
        }

        documentViewRepository.insertUserDocumentView(
                request.getUserId(), document.getDocumentId(),
                document.getName(), document.getSummary(), document.getContent(), imageIds.size(), now);
        documentViewRepository.markDocumented(request.getUserId(), imageIds);

        request.complete(document.getDocumentId(), now);

        log.info("문서 저장 완료: documentRequestId={} documentId={} images={}",
                request.getId(), document.getDocumentId(), imageIds.size());
    }

    /**
     * 재분석 결과 반영. 새 문서를 만들지 않고 기존 document_id의 내용과 관계를 갈아끼운다.
     *
     * <p>관계는 통째로 지우고 다시 넣는 대신 diff로 처리한다 — 유지되는 이미지의
     * image_document 행이 그대로 남아야 조회 모델의 PK(image_document_id)도 유지되고,
     * "언제 이 문서에 들어왔는지"가 재분석마다 초기화되지 않는다.
     */
    private void applyRegeneration(DocumentGenerationRequest request, DocumentCompletedPayload payload,
                                   List<Integer> imageIds, OffsetDateTime now) {
        Integer userId = request.getUserId();
        Integer documentId = request.getSourceDocumentId();

        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null || "Y".equals(document.getDelYn())) {
            // 재분석 중에 사용자가 문서를 지운 경우. 되살리면 "지웠는데 다시 나타나는" 문서가
            // 되므로 요청만 실패로 닫는다.
            log.warn("재분석 대상 문서가 없어 결과를 버린다: documentRequestId={} documentId={}",
                    request.getId(), documentId);
            request.fail("DOCUMENT_DELETED", now);
            return;
        }

        document.update(payload.title(), payload.summary(), payload.markdown(), now);

        List<Integer> removed = documentCommandRepository.findActiveImageIds(documentId).stream()
                .filter(imageId -> !imageIds.contains(imageId))
                .toList();
        documentCommandRepository.softDeleteRelations(userId, documentId, removed, now);

        for (Integer imageId : imageIds) {
            Integer imageDocumentId = documentCommandRepository.upsertRelation(documentId, imageId, now);
            boolean copied = documentCommandRepository.upsertDocumentImageView(
                    imageDocumentId, userId, documentId, imageId, now);
            if (!copied) {
                log.warn("조회 모델에 복사할 이미지를 찾지 못했다: documentId={} imageId={}", documentId, imageId);
            }
        }

        documentViewRepository.updateUserDocumentView(
                userId, documentId,
                document.getName(), document.getSummary(), document.getContent(), imageIds.size(), now);
        documentViewRepository.markDocumented(userId, imageIds);
        documentViewRepository.unmarkDocumentedIfOrphan(userId, removed);

        request.complete(documentId, now);

        log.info("문서 재분석 반영 완료: documentRequestId={} documentId={} images={} removed={}",
                request.getId(), documentId, imageIds.size(), removed.size());
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
