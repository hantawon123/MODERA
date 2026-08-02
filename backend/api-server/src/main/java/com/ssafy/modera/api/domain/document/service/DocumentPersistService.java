package com.ssafy.modera.api.domain.document.service;

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
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeOutboxService;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 문서 원본·관계·조회 모델 저장. 두 경로가 공유한다 — 동기 생성(8-2)과, 아직 남아 있는
 * 이벤트 수신 경로(DocumentResultEventHandler).
 *
 * <p>전부 한 트랜잭션이다. 하나라도 실패하면 요청은 QUEUED로 남고 문서도 만들어지지
 * 않는다 — "문서는 있는데 관계가 없는" 중간 상태를 남기지 않는 게 이 경계의 목적이다.
 *
 * <p>AI 호출은 이 트랜잭션 밖에서 끝나 있어야 한다. 수십 초 걸리는 호출을 트랜잭션
 * 안에 두면 그동안 커넥션을 잡고 있게 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentPersistService {

    private final DocumentGenerationRequestRepository documentGenerationRequestRepository;
    private final DocumentRepository documentRepository;
    private final UserDocumentRepository userDocumentRepository;
    private final ImageDocumentRepository imageDocumentRepository;
    private final DocumentViewRepository documentViewRepository;
    private final DocumentCommandRepository documentCommandRepository;
    private final UserDataChangeOutboxService userDataChangeOutboxService;

    /**
     * @return 저장된 문서 ID. 재분석 대상 문서가 그 사이 삭제됐으면 null(요청은 실패로 닫는다).
     */
    @Transactional
    public Integer persist(Integer requestId, DocumentGenerationResult result) {
        DocumentGenerationRequest request = documentGenerationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalStateException(
                        "문서 생성 요청을 찾을 수 없습니다: requestId=" + requestId));

        OffsetDateTime now = OffsetDateTime.now();
        List<Integer> imageIds = result.imageIds() == null ? List.of() : result.imageIds();

        return request.isRegeneration()
                ? applyRegeneration(request, result, imageIds, now)
                : createDocument(request, result, imageIds, now);
    }

    @Transactional
    public void markFailed(Integer requestId, String reason) {
        documentGenerationRequestRepository.findById(requestId)
                .ifPresent(request -> request.fail(reason, OffsetDateTime.now()));
    }

    private Integer createDocument(DocumentGenerationRequest request, DocumentGenerationResult result,
                                   List<Integer> imageIds, OffsetDateTime now) {
        Document document = documentRepository.save(
                new Document(result.title(), result.summary(), result.markdown(), now));
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
        userDataChangeOutboxService.record(
                request.getUserId(), UserDataChangeResource.DOCUMENT,
                String.valueOf(document.getDocumentId()));

        log.info("문서 저장 완료: documentRequestId={} documentId={} images={}",
                request.getId(), document.getDocumentId(), imageIds.size());
        return document.getDocumentId();
    }

    /**
     * 재분석 결과 반영. 새 문서를 만들지 않고 기존 document_id의 내용과 관계를 갈아끼운다.
     *
     * <p>관계는 통째로 지우고 다시 넣는 대신 diff로 처리한다 — 유지되는 이미지의
     * image_document 행이 그대로 남아야 조회 모델의 PK(image_document_id)도 유지되고,
     * "언제 이 문서에 들어왔는지"가 재분석마다 초기화되지 않는다.
     */
    private Integer applyRegeneration(DocumentGenerationRequest request, DocumentGenerationResult result,
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
            return null;
        }

        document.update(result.title(), result.summary(), result.markdown(), now);

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
        userDataChangeOutboxService.record(
                userId, UserDataChangeResource.DOCUMENT, String.valueOf(documentId));

        log.info("문서 재분석 반영 완료: documentRequestId={} documentId={} images={} removed={}",
                request.getId(), documentId, imageIds.size(), removed.size());
        return documentId;
    }
}
