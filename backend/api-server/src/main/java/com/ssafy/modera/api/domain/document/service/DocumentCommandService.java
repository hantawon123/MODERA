package com.ssafy.modera.api.domain.document.service;

import com.ssafy.modera.api.domain.document.dto.request.DocumentCreateRequest;
import com.ssafy.modera.api.domain.document.dto.response.DocumentGenerationAcceptedResponse;
import com.ssafy.modera.api.domain.document.entity.DocumentGenerationRequest;
import com.ssafy.modera.api.domain.document.exception.DocumentErrorCode;
import com.ssafy.modera.api.domain.document.repository.DocumentGenerationRequestRepository;
import com.ssafy.modera.api.domain.event.EventPublisher;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.DocumentSourceImage;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.DocumentRequestedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 문서 생성 요청 접수(8-2).
 *
 * <p>실제 생성은 analysis-worker가 AI를 호출해 수행하고, 이 서비스는 검증·요청 이력 기록·
 * 이벤트 발행까지만 한다. 결과 저장은 {@code DocumentResultEventHandler} 몫이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentCommandService {

    /**
     * 한 문서에 넣을 수 있는 이미지 수. AI 서버의 MAX_IMAGES와 같은 값이다 — 넘겨 보내면
     * AI가 400으로 거절하고 worker가 DOCUMENT_AI_REJECTED로 실패시키므로, 사용자에게
     * 바로 400을 주는 편이 낫다. AI 쪽 상한이 바뀌면 이 값도 같이 맞춘다.
     */
    private static final int MAX_IMAGES = 30;

    private static final String ANALYSIS_STATUS_COMPLETED = "COMPLETED";

    private final ImageQueryRepository imageQueryRepository;
    private final DocumentGenerationRequestRepository documentGenerationRequestRepository;
    private final EventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    public DocumentGenerationAcceptedResponse create(Integer userId, DocumentCreateRequest request) {
        List<Integer> imageIds = validateImageIds(request.imageIds());
        List<DocumentSourceImage> sources = loadSources(userId, imageIds);

        DocumentGenerationRequest saved = enqueue(userId, request);

        // 커밋이 끝난 뒤에 발행한다. 트랜잭션 안에서 발행하면 커밋이 실패했을 때 이미 나간
        // 이벤트를 되돌릴 수 없어, worker가 존재하지 않는 요청의 문서를 만들게 된다.
        publish(saved, sources);

        return new DocumentGenerationAcceptedResponse(
                saved.getClientRequestId(), saved.getStatus());
    }

    /** 중복·개수만 본다. 소유권·분석 완료는 조회 결과로 판단한다. */
    private List<Integer> validateImageIds(List<Integer> imageIds) {
        Set<Integer> unique = new HashSet<>(imageIds);
        if (unique.size() != imageIds.size()) {
            throw new BusinessException(DocumentErrorCode.INVALID_DOCUMENT_IMAGES);
        }
        if (imageIds.size() > MAX_IMAGES) {
            throw new BusinessException(DocumentErrorCode.INVALID_DOCUMENT_IMAGES);
        }
        if (imageIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(DocumentErrorCode.INVALID_DOCUMENT_IMAGES);
        }
        return imageIds;
    }

    /**
     * 재료를 읽으면서 소유권과 분석 완료 여부를 함께 검증한다.
     *
     * <p>조회 자체가 소유권 검증이다 — user_image 조인이 걸려 있어 남의 이미지나 삭제된
     * 이미지는 행이 돌아오지 않는다. 그래서 "요청한 개수 ≠ 돌아온 개수"면 소유하지 않은
     * 것이 섞여 있다는 뜻이고, 존재하지 않는 ID인지 남의 것인지는 구분하지 않는다
     * (구분해 알려주면 남의 imageId를 넣어보며 존재를 탐색할 수 있다).
     */
    private List<DocumentSourceImage> loadSources(Integer userId, List<Integer> imageIds) {
        List<DocumentSourceImage> found = imageQueryRepository.findDocumentSources(userId, imageIds);
        if (found.size() != imageIds.size()) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_IMAGE_NOT_OWNED);
        }

        boolean hasUnanalyzed = found.stream()
                .anyMatch(source -> !ANALYSIS_STATUS_COMPLETED.equals(source.analysisStatus()));
        if (hasUnanalyzed) {
            // 분석 전 이미지는 요약·OCR이 비어 AI가 NO_CONTENT로 건너뛴다. 전부 건너뛰면
            // AI가 400을 돌려주므로, 문서를 만들다 실패하기 전에 여기서 끊는다.
            throw new BusinessException(ImageErrorCode.IMAGE_ANALYSIS_NOT_COMPLETED);
        }

        return sortByRequestOrder(imageIds, found);
    }

    /**
     * 클라이언트가 준 순서로 되돌린다. IN 절 조회는 순서를 보장하지 않는데,
     * 이벤트 payload의 이미지 순서는 "첫 번째가 중심 자료"라는 의미를 갖는다.
     */
    private List<DocumentSourceImage> sortByRequestOrder(
            List<Integer> imageIds, List<DocumentSourceImage> found) {
        Map<Integer, DocumentSourceImage> byId = new LinkedHashMap<>();
        for (DocumentSourceImage source : found) {
            byId.put(source.imageId(), source);
        }
        return imageIds.stream().map(byId::get).toList();
    }

    /**
     * 요청 이력을 남긴다. UNIQUE(user_id, client_request_id)가 최종 방어선이지만, 먼저
     * 조회해 409를 돌려줘야 "왜 거부됐는지"가 응답에 드러난다.
     */
    private DocumentGenerationRequest enqueue(Integer userId, DocumentCreateRequest request) {
        return transactionTemplate.execute(status -> {
            documentGenerationRequestRepository
                    .findByUserIdAndClientRequestIdAndDelYn(userId, request.clientRequestId(), "N")
                    .ifPresent(existing -> {
                        throw new BusinessException(DocumentErrorCode.DUPLICATE_CLIENT_REQUEST);
                    });

            return documentGenerationRequestRepository.save(
                    DocumentGenerationRequest.create(userId, request.clientRequestId(), OffsetDateTime.now()));
        });
    }

    /**
     * 발행이 실패하면 요청을 즉시 FAILED로 확정한다. 그대로 두면 QUEUED인 채로 영원히
     * 남아 사용자가 아무 결과도 받지 못한다(아직 QUEUED 정체를 훑는 배치가 없다).
     */
    private void publish(DocumentGenerationRequest saved, List<DocumentSourceImage> sources) {
        DocumentRequestedPayload payload = new DocumentRequestedPayload(
                String.valueOf(saved.getId()),
                saved.getUserId(),
                null,   // instruction: 8-2에 사용자 지시문이 없다. worker가 기본 문구로 채운다.
                sources.stream().map(this::toPayloadImage).toList()
        );

        try {
            eventPublisher.publish(Streams.IMAGE_ANALYSIS, EventTypes.DOCUMENT_REQUESTED, 1, payload);
        } catch (Exception exception) {
            log.error("DOCUMENT_REQUESTED 발행 실패 — 요청을 FAILED로 확정한다: documentRequestId={}",
                    saved.getId(), exception);
            transactionTemplate.executeWithoutResult(status ->
                    documentGenerationRequestRepository.findById(saved.getId())
                            .ifPresent(request -> request.fail("EVENT_PUBLISH_FAILED", OffsetDateTime.now())));
            throw exception;
        }

        log.info("DOCUMENT_REQUESTED 발행: documentRequestId={} userId={} images={}",
                saved.getId(), saved.getUserId(), payload.images().size());
    }

    private DocumentRequestedPayload.SourceImage toPayloadImage(DocumentSourceImage source) {
        return new DocumentRequestedPayload.SourceImage(
                source.imageId(),
                source.title(),
                source.categoryName(),
                source.tagNames(),
                source.keyInformation(),
                source.summary(),
                source.uploadedAt() == null ? null : source.uploadedAt().toString()
        );
    }
}
