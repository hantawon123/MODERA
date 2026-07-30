package com.ssafy.modera.api.domain.document.service;

import com.ssafy.modera.api.domain.document.client.DocumentAiClient;
import com.ssafy.modera.api.domain.document.dto.request.DocumentCreateRequest;
import com.ssafy.modera.api.domain.document.dto.request.DocumentRegenerateRequest;
import com.ssafy.modera.api.domain.document.dto.response.DocumentDetailResponse;
import com.ssafy.modera.api.domain.document.entity.DocumentGenerationRequest;
import com.ssafy.modera.api.domain.document.exception.DocumentErrorCode;
import com.ssafy.modera.api.domain.document.repository.DocumentGenerationRequestRepository;
import com.ssafy.modera.api.domain.document.repository.DocumentQueryRepository;
import com.ssafy.modera.api.domain.image.entity.Ocr;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.DocumentSourceImage;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.OcrRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 문서 생성·재분석. <b>동기 처리</b>다 — 요청이 끝나야 응답이 나가고, 응답 본문에 완성된
 * 문서가 실린다.
 *
 * <p>원래는 이벤트로 worker에 넘겨 202로 접수만 알리는 비동기였다. 동기로 바꾼 이유는
 * 결과를 사용자에게 전달할 길이 없었기 때문이다 — 완료 알림(FCM)도, 진행 상태를 물어볼
 * 창구도 없어서 앱은 "언젠가 목록에 나타나는" 문서를 기다려야 했다. 동기 응답이면
 * 폴링도 알림도 필요 없고, 실패를 그 자리에서 에러로 돌려줄 수 있다.
 *
 * <p>worker를 경유하지 않는 이유: 경유의 근거가 "OCR이 worker DB에만 있다"였는데, 앱이
 * 보낸 OCR 원문은 api의 image_schema.ocr에도 그대로 있다. 재료가 전부 여기 있으니 한
 * 홉을 줄인다.
 *
 * <p><b>트레이드오프</b>: 요청 스레드가 LLM 생성이 끝날 때까지(수 초~수십 초) 묶인다.
 * 문서 생성은 드문 요청이라 MVP에서는 허용한다. read 타임아웃 90초는 AiClientConfig에
 * 있고, 그보다 오래 걸리면 실패로 끊는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentCommandService {

    /**
     * 한 문서에 넣을 수 있는 이미지 수. AI 서버의 MAX_IMAGES와 같은 값이다 — 넘겨 보내면
     * AI가 400으로 거절하므로, 사용자에게 바로 400을 주는 편이 낫다. AI 쪽 상한이 바뀌면
     * 이 값도 같이 맞춘다.
     */
    private static final int MAX_IMAGES = 30;

    private static final String ANALYSIS_STATUS_COMPLETED = "COMPLETED";

    /**
     * 지시문 기본값. AI가 빈 instruction에 422를 돌려주므로 채워 보낸다(사용자가 지시문
     * 없이 버튼만 누르는 게 정상 UX다).
     */
    private static final String DEFAULT_INSTRUCTION = "선택한 자료들을 정리해 하나의 문서로 만들어 줘";

    private final ImageQueryRepository imageQueryRepository;
    private final DocumentQueryRepository documentQueryRepository;
    private final DocumentGenerationRequestRepository documentGenerationRequestRepository;
    private final OcrRepository ocrRepository;
    private final DocumentAiClient documentAiClient;
    private final DocumentPersistService documentPersistService;
    private final DocumentQueryService documentQueryService;
    private final TransactionTemplate transactionTemplate;

    public DocumentDetailResponse create(Integer userId, DocumentCreateRequest request) {
        List<Integer> imageIds = validateImageIds(request.imageIds());
        List<DocumentSourceImage> sources = loadSources(userId, imageIds);

        DocumentGenerationRequest saved = enqueue(userId, request);

        return generate(saved, userId, sources);
    }

    /**
     * 재분석. 완료되면 새 문서를 만들지 않고 대상 문서를 갱신한다.
     *
     * <p>imageIds를 생략하면 현재 구성 이미지를 그대로 쓴다(내용만 다시 정리). 값을 주면
     * 그것이 최종 구성이 되므로 이미지 추가·제외도 같은 경로로 처리된다.
     */
    public DocumentDetailResponse regenerate(
            Integer userId, Integer documentId, DocumentRegenerateRequest request) {
        if (!documentQueryRepository.existsDocument(userId, documentId)) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        List<Integer> requestedImageIds =
                request.imageIds() == null || request.imageIds().isEmpty()
                        ? documentQueryRepository.findDocumentImageIds(userId, documentId)
                        : request.imageIds();
        // 재료가 하나도 없는 문서는 만들 수 없다 — 구성 이미지가 전부 삭제된 문서를 그대로
        // 재분석하려 할 때 여기 걸린다(AI에 보내봐야 400으로 돌아온다).
        if (requestedImageIds.isEmpty()) {
            throw new BusinessException(DocumentErrorCode.INVALID_DOCUMENT_IMAGES);
        }

        List<Integer> imageIds = validateImageIds(requestedImageIds);
        List<DocumentSourceImage> sources = loadSources(userId, imageIds);

        DocumentGenerationRequest saved = enqueueRegeneration(userId, documentId, request);

        return generate(saved, userId, sources);
    }

    /**
     * AI 호출 → 저장 → 완성된 문서 반환.
     *
     * <p>트랜잭션 밖에서 AI를 부른다. 호출이 수십 초 걸리므로 트랜잭션 안에 두면 그동안
     * DB 커넥션을 붙잡고 있게 된다. 요청 이력 저장(앞)과 결과 저장(뒤)이 각각 자기
     * 트랜잭션을 갖는다.
     */
    private DocumentDetailResponse generate(
            DocumentGenerationRequest saved, Integer userId, List<DocumentSourceImage> sources) {
        DocumentAiClient.DocumentResponse response;
        try {
            response = documentAiClient.generate(toAiRequest(userId, sources));
        } catch (HttpClientErrorException exception) {
            // 4xx는 요청 자체가 거부된 것이라 같은 입력으로 다시 눌러도 결과가 같다.
            log.warn("문서 생성 요청이 거부됨(4xx): documentRequestId={} status={}",
                    saved.getId(), exception.getStatusCode());
            documentPersistService.markFailed(saved.getId(), "DOCUMENT_AI_REJECTED");
            throw new BusinessException(DocumentErrorCode.DOCUMENT_AI_REJECTED);
        } catch (RestClientException exception) {
            // 5xx·타임아웃·연결 실패. 시간이 지나면 나을 수 있어 재시도할 가치가 있다.
            log.warn("문서 생성 호출 실패(장애·타임아웃): documentRequestId={} cause={}",
                    saved.getId(), exception.getMessage());
            documentPersistService.markFailed(saved.getId(), "DOCUMENT_AI_ERROR");
            throw new BusinessException(DocumentErrorCode.DOCUMENT_GENERATION_FAILED);
        }

        if (response == null || response.markdown() == null || response.markdown().isBlank()) {
            // 2xx인데 본문이 비었다. "완료인데 내용 없음"을 빈 문서로 저장하지 않는다.
            log.warn("문서 생성 응답에 markdown이 없다: documentRequestId={}", saved.getId());
            documentPersistService.markFailed(saved.getId(), "DOCUMENT_EMPTY_RESULT");
            throw new BusinessException(DocumentErrorCode.DOCUMENT_GENERATION_FAILED);
        }

        List<Integer> usedImageIds = response.sourceImageIds() == null
                ? sources.stream().map(DocumentSourceImage::imageId).toList()
                : response.sourceImageIds();

        Integer documentId = documentPersistService.persist(saved.getId(), new DocumentGenerationResult(
                response.title(), response.summary(), response.markdown(), usedImageIds));
        if (documentId == null) {
            // 재분석 중에 문서가 삭제된 경우. 저장할 곳이 사라졌다.
            throw new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND);
        }

        return documentQueryService.getDocument(userId, documentId);
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
     * 이미지 순서는 "첫 번째가 중심 자료"라는 의미를 갖고 AI에 그대로 전달된다.
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
     * 조회 모델의 재료 + 앱이 보낸 OCR 원문을 AI 요청으로 옮긴다.
     *
     * <p>null을 그대로 넘기지 않고 전부 기본값으로 채우는 게 요점이다 — AI의 DocumentImage는
     * 이 필드들을 "기본값 있는 필수 필드"로 선언해서, 명시적 null이 오면 요청 전체를
     * 400으로 거절한다(DocumentAiClient.SourceImage javadoc 참고).
     */
    private DocumentAiClient.DocumentRequest toAiRequest(
            Integer userId, List<DocumentSourceImage> sources) {
        List<Integer> imageIds = sources.stream().map(DocumentSourceImage::imageId).toList();
        Map<Integer, String> ocrByImageId = ocrRepository.findByImageIdIn(imageIds).stream()
                .collect(Collectors.toMap(Ocr::getImageId, ocr -> ocr.getContent() == null ? "" : ocr.getContent(),
                        (first, second) -> first));

        List<DocumentAiClient.SourceImage> images = sources.stream()
                .map(source -> new DocumentAiClient.SourceImage(
                        source.imageId(),
                        nullToEmpty(source.title()),
                        nullToEmpty(source.summary()),
                        nullToEmpty(source.tagNames()),
                        source.categoryName(),
                        nullToEmpty(source.keyInformation()),
                        new DocumentAiClient.Ocr(ocrByImageId.getOrDefault(source.imageId(), "")),
                        source.uploadedAt() == null ? null : source.uploadedAt().toString()
                ))
                .toList();

        return new DocumentAiClient.DocumentRequest(
                userId,
                images,
                null,                  // title: 모델이 정한다
                DEFAULT_INSTRUCTION,   // 8-2에 사용자 지시문이 없다
                null                   // language: 기본(한국어 프롬프트)
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<String> nullToEmpty(List<String> value) {
        return value == null ? List.of() : value;
    }

    /**
     * 요청 이력을 남긴다. UNIQUE(user_id, client_request_id)가 최종 방어선이지만, 먼저
     * 조회해 409를 돌려줘야 "왜 거부됐는지"가 응답에 드러난다.
     *
     * <p>AI 호출 전에 커밋해 두는 이유는 이력이 곧 중복 방지 장치라서다 — 같은
     * clientRequestId로 두 번째 요청이 들어오면 첫 요청이 아직 AI를 기다리는 중이어도
     * 막혀야 한다.
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
     * 재분석 요청 이력. 중복 clientRequestId 차단은 8-2와 같고, 여기에 "이 문서에 이미
     * 진행 중인 재분석이 있는가"가 더 붙는다.
     *
     * <p>조회로 먼저 끊고도 제약 위반을 따로 잡는 이유는 동시 요청 때문이다 — 두 요청이
     * 나란히 조회를 통과할 수 있고, 그때 최종적으로 막아 주는 건 036의 부분 유니크
     * 인덱스다. 그 예외를 그대로 두면 500이 나가므로 같은 409로 번역한다.
     */
    private DocumentGenerationRequest enqueueRegeneration(
            Integer userId, Integer documentId, DocumentRegenerateRequest request) {
        if (documentGenerationRequestRepository.existsByUserIdAndSourceDocumentIdAndStatusAndDelYn(
                userId, documentId, DocumentGenerationRequest.STATUS_QUEUED, "N")) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_REGENERATION_IN_PROGRESS);
        }

        return transactionTemplate.execute(status -> {
            documentGenerationRequestRepository
                    .findByUserIdAndClientRequestIdAndDelYn(userId, request.clientRequestId(), "N")
                    .ifPresent(existing -> {
                        throw new BusinessException(DocumentErrorCode.DUPLICATE_CLIENT_REQUEST);
                    });

            try {
                return documentGenerationRequestRepository.saveAndFlush(
                        DocumentGenerationRequest.regenerate(
                                userId, request.clientRequestId(), documentId, OffsetDateTime.now()));
            } catch (DataIntegrityViolationException exception) {
                throw new BusinessException(DocumentErrorCode.DOCUMENT_REGENERATION_IN_PROGRESS);
            }
        });
    }
}
