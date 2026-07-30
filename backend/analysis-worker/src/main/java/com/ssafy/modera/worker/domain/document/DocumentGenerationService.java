package com.ssafy.modera.worker.domain.document;

import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.DocumentCompletedPayload;
import com.ssafy.modera.contract.payload.DocumentFailedPayload;
import com.ssafy.modera.contract.payload.DocumentRequestedPayload;
import com.ssafy.modera.worker.domain.document.client.DocumentAiClient;
import com.ssafy.modera.worker.domain.document.repository.DocumentOcrRepository;
import com.ssafy.modera.worker.domain.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 문서 생성(마크다운) 중개.
 *
 * worker는 문서를 저장하지 않는다 — payload의 재료에 자기 DB의 OCR
 * (analysis_result.ocr_refined_text)만 보태 AI를 동기 호출하고, 결과를
 * DOCUMENT_COMPLETED로 되돌려 보낸다. OCR이 worker DB에만 있어 worker가
 * 경유지가 될 뿐, 저장(document_schema)은 api-server 몫이다.
 *
 * <p><b>블로킹 트레이드오프</b>: 이 동기 호출(수 초~수십 초)은 이미지 분석과 같은
 * 단일 컨슈머 스레드를 점유한다 — 문서를 만드는 동안 이미지 분석 이벤트 소비가
 * 지연된다. 문서 요청은 드물어 MVP에서는 허용한다.
 * TODO: 볼륨이 늘면 전용 executor로 빼거나 스트림을 분리한다.
 *
 * <p>예외 정책: AI 호출 실패는 여기서 삼키고 DOCUMENT_FAILED를 발행한 뒤 정상
 * 리턴한다(→ 컨슈머가 XACK). 자동 재시도는 없다 — 재시도는 곧 LLM 비용이고,
 * 사용자가 버튼을 다시 누르는 게 재시도다. 반면 OCR 조회·이벤트 발행이 던지는
 * 인프라 예외(DB/Redis)는 그대로 올린다 — 컨슈머의 일시 오류 정책(XACK 보류,
 * PEL 재전달)을 타야 하는 종류라서다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentGenerationService {

    /**
     * instruction이 비었을 때의 기본 지시문. AI가 빈 instruction에 422를 돌려주므로
     * 여기서 채워 보낸다(사용자가 지시문 없이 버튼만 누르는 게 정상 UX다).
     */
    private static final String DEFAULT_INSTRUCTION = "선택한 자료들을 정리해 하나의 문서로 만들어 줘";

    /** 요청 자체가 거부됨(4xx) — 같은 입력으로 다시 눌러도 똑같이 거부된다. */
    private static final String ERROR_AI_REJECTED = "DOCUMENT_AI_REJECTED";
    /** AI 장애·타임아웃(5xx 등) — 시간이 지나면 나을 수 있어 다시 눌러볼 가치가 있다. */
    private static final String ERROR_AI_ERROR = "DOCUMENT_AI_ERROR";
    /** 2xx인데 markdown이 비어 있음 — "완료인데 내용 없음"을 빈 문서로 저장하지 않게 실패로 끊는다. */
    private static final String ERROR_EMPTY_DOCUMENT = "DOCUMENT_EMPTY_RESULT";

    private static final int MAX_ERROR_MESSAGE = 500;

    private final DocumentOcrRepository documentOcrRepository;
    private final DocumentAiClient documentAiClient;
    private final EventPublisher eventPublisher;

    public void handle(DocumentRequestedPayload payload) {
        // 멱등키가 없으면 처리 자체를 거부한다(FAILED도 발행하지 않는다 — 어느 요청의
        // 실패인지 특정할 수 없다). UUID 같은 폴백을 worker가 대신 만들면 안 된다:
        // PEL 재처리로 같은 이벤트가 다시 오면 매번 다른 키가 생겨 api가 같은 문서를
        // 중복 저장한다. 멱등키는 발행자(api)만 만들 수 있고, 빼먹는 건 계약 위반이라
        // 거부가 맞다.
        if (payload.documentRequestId() == null || payload.documentRequestId().isBlank()) {
            log.error("DOCUMENT_REQUESTED에 documentRequestId가 없어 처리를 거부한다(계약 위반): userId={}",
                    payload.userId());
            return;
        }

        String instruction = payload.instruction() == null || payload.instruction().isBlank()
                ? DEFAULT_INSTRUCTION
                : payload.instruction();

        List<DocumentRequestedPayload.SourceImage> images =
                payload.images() == null ? List.of() : payload.images();
        List<Integer> imageIds = images.stream()
                .map(DocumentRequestedPayload.SourceImage::imageId)
                .toList();

        // 분석 이력이 없는 이미지는 맵에 없다 → ocr null로 실려 AI가 skipped 처리한다.
        Map<Integer, String> ocrByImageId = documentOcrRepository.findLatestRefinedOcr(imageIds);

        DocumentAiClient.DocumentRequest request = new DocumentAiClient.DocumentRequest(
                payload.userId(),
                images.stream().map(image -> toSourceImage(image, ocrByImageId)).toList(),
                null,           // title: 모델이 정한다
                instruction,
                null            // language: 기본(한국어 프롬프트)
        );

        DocumentAiClient.DocumentResponse response;
        try {
            response = documentAiClient.generate(request);
        } catch (HttpClientErrorException e) {
            log.warn("문서 생성 요청 거부됨(4xx): documentRequestId={} status={}",
                    payload.documentRequestId(), e.getStatusCode());
            publishFailed(payload, ERROR_AI_REJECTED, e.getMessage(), false);
            return;
        } catch (RestClientException e) {
            log.warn("문서 생성 호출 실패(장애·타임아웃): documentRequestId={} cause={}",
                    payload.documentRequestId(), e.getMessage());
            publishFailed(payload, ERROR_AI_ERROR, e.getMessage(), true);
            return;
        }

        if (response == null || response.markdown() == null || response.markdown().isBlank()) {
            log.warn("문서 생성 응답에 markdown이 없다 — 실패로 처리: documentRequestId={}",
                    payload.documentRequestId());
            publishFailed(payload, ERROR_EMPTY_DOCUMENT, "AI 응답에 markdown이 비어 있음", true);
            return;
        }

        List<DocumentCompletedPayload.Section> sections = toSections(response.sections());

        DocumentCompletedPayload completed = new DocumentCompletedPayload(
                payload.documentRequestId(),
                payload.userId(),
                response.title(),
                response.summary(),
                response.markdown(),
                sections,
                // AI가 sourceImageIds를 생략하면 요청 재료 전체를 쓴 것으로 간주한다.
                response.sourceImageIds() == null ? imageIds : response.sourceImageIds(),
                response.modelVersion(),
                response.generatedAt() == null ? Instant.now().toString() : response.generatedAt()
        );
        eventPublisher.publish(Streams.ANALYSIS_RESULT, EventTypes.DOCUMENT_COMPLETED, 1, completed);
        log.info("DOCUMENT_COMPLETED 발행: documentRequestId={} images={} markdown={}자 sections={}개",
                payload.documentRequestId(), imageIds.size(), response.markdown().length(), sections.size());
    }

    /**
     * AI의 sections를 payload Section으로 옮긴다. 이름만 바꾸고 내용은 가공하지 않는다 —
     * bullets를 contentText에 합치는 식의 손질을 하면 클라이언트가 구조를 복원할 수 없다.
     *
     * <p>sections가 없어도 실패로 보지 않는다. 문서의 최종 산출물은 markdown이고 sections는
     * 화면 구성을 돕는 부가 정보라, 기존 정책(markdown만 있으면 COMPLETED)을 그대로 둔다.
     *
     * <p>sequence는 AI가 주지 않으므로 여기서 1부터 붙인다. 값이 비어 원소를 건너뛰더라도
     * 번호가 이어지도록 인덱스가 아니라 담긴 개수를 기준으로 센다.
     */
    private List<DocumentCompletedPayload.Section> toSections(List<DocumentAiClient.Section> sections) {
        if (sections == null || sections.isEmpty()) {
            return List.of();
        }

        List<DocumentCompletedPayload.Section> converted = new ArrayList<>(sections.size());
        for (DocumentAiClient.Section section : sections) {
            if (section == null) {
                continue;
            }
            converted.add(new DocumentCompletedPayload.Section(
                    converted.size() + 1,
                    section.heading(),
                    section.body(),
                    section.bullets() == null ? List.of() : section.bullets(),
                    section.imageIds() == null ? List.of() : section.imageIds()
            ));
        }
        return converted;
    }

    private DocumentAiClient.SourceImage toSourceImage(
            DocumentRequestedPayload.SourceImage image, Map<Integer, String> ocrByImageId) {
        String refinedOcr = ocrByImageId.get(image.imageId());
        return new DocumentAiClient.SourceImage(
                image.imageId(),
                image.title(),
                image.summary(),
                image.tagNames(),      // AI 스키마 필드명은 tags
                image.categoryName(),  // AI 스키마 필드명은 category
                image.keyInformation(),
                refinedOcr == null || refinedOcr.isBlank() ? null : new DocumentAiClient.Ocr(refinedOcr),
                image.createdAt()
        );
    }

    private void publishFailed(DocumentRequestedPayload payload, String errorCode,
                               String errorMessage, boolean retryable) {
        eventPublisher.publish(Streams.ANALYSIS_RESULT, EventTypes.DOCUMENT_FAILED, 1,
                new DocumentFailedPayload(
                        payload.documentRequestId(),
                        payload.userId(),
                        errorCode,
                        truncate(errorMessage),
                        retryable));
        log.warn("DOCUMENT_FAILED 발행: documentRequestId={} code={} retryable={}",
                payload.documentRequestId(), errorCode, retryable);
    }

    private String truncate(String message) {
        if (message == null) {
            return "unknown";
        }
        return message.length() <= MAX_ERROR_MESSAGE ? message : message.substring(0, MAX_ERROR_MESSAGE);
    }
}
