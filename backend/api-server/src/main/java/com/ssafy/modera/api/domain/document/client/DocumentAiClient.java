package com.ssafy.modera.api.domain.document.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * AI 서버의 문서 생성(마크다운) 동기 호출.
 *
 * <p>analysis-worker에도 같은 이름의 클라이언트가 있지만 공유하지 않는다 — 두 서버가
 * 공유할 수 있는 건 event-contract뿐이라는 규칙(CLAUDE.md) 때문이고, 실제로도 worker
 * 쪽은 이벤트 경유 경로가 남아 있어 수명이 다르다.
 *
 * <p>예외를 삼키지 않는다. 연관 이미지 조회(WorkerSearchClient)는 실패해도 빈 목록으로
 * degrade하는 게 맞지만, 문서 생성은 이 호출이 곧 요청의 목적이라 실패를 숨기면 사용자가
 * "성공했는데 문서가 없는" 상태를 보게 된다. 분류는 호출자가 한다.
 */
@Component
public class DocumentAiClient {

    private final RestClient aiRestClient;

    public DocumentAiClient(@Qualifier("aiRestClient") RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    public DocumentResponse generate(DocumentRequest request) {
        return aiRestClient.post()
                .uri("/internal/v1/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(DocumentResponse.class);
    }

    // ── AI 서버 요청/응답 스키마 (ai/ai_main/app/schemas.py의 DocumentRequest/
    // DocumentImage/DocumentResponse 기준, camelCase) ──

    public record DocumentRequest(
            Integer userId,
            List<SourceImage> images,
            String title,          // null이면 모델이 제목을 정한다
            String instruction,
            String language        // null 허용
    ) {}

    /**
     * AI 스키마 필드명은 tags/category다(우리 조회 모델의 tagNames/categoryName과 다름).
     *
     * <p><b>어떤 필드도 null로 보내면 안 된다</b>: AI의 DocumentImage는 title·summary·
     * tags·keyInformation·ocr을 "Optional이 아니고 기본값만 있는" 필드로 선언한다.
     * 필드를 생략하면 기본값이 들어가지만 명시적 null은 검증 실패라, 요청 전체가
     * INVALID_REQUEST(400)로 거절된다. category·createdAt만 null이어도 된다.
     */
    public record SourceImage(
            Integer imageId,
            String title,
            String summary,
            List<String> tags,
            String category,
            List<String> keyInformation,
            Ocr ocr,               // null 금지 — 텍스트가 없으면 rawText=""인 빈 Ocr을 보낸다
            String createdAt
    ) {}

    /**
     * 우리가 가진 값은 앱의 온디바이스 OCR 원문(image_schema.ocr.content)이라 rawText에
     * 싣는다. AI는 refinedText가 있으면 그쪽을 우선 쓰지만 우리에겐 정제본이 없다.
     */
    public record Ocr(String rawText) {}

    /** 응답에는 skipped(내용이 없어 건너뛴 이미지)도 오지만 저장에 쓰지 않는다. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DocumentResponse(
            String title,
            String summary,
            String markdown,
            List<Integer> sourceImageIds,
            String modelVersion,
            String generatedAt
    ) {}
}
