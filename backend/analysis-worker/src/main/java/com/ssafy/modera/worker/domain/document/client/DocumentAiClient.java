package com.ssafy.modera.worker.domain.document.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * AI 서버의 문서 생성(마크다운) 동기 호출.
 *
 * FastApiAnalysisClient와 같은 서버·같은 토큰을 쓰지만 성격이 다르다 — 분석은
 * 202 접수 후 콜백이고, 문서 생성은 응답 본문에 결과가 실려 오는 동기 호출이다.
 * 그래서 콜백 URL도, job 테이블도 없다.
 *
 * 타임아웃이 필수다: 이 호출은 이미지 분석과 같은 단일 컨슈머 스레드 위에서
 * 실행되므로(ImageAnalysisConsumer 참고), 여기가 매달리면 이미지 분석 소비까지
 * 전부 멈춘다. connect 3s / read 90s — LLM 생성이 수십 초까지 갈 수 있어
 * read는 넉넉히, 대신 무한은 절대 아니게.
 */
@Slf4j
@Component
public class DocumentAiClient {

    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(90);

    private final RestClient restClient;
    private final String internalToken;

    public DocumentAiClient(
            @Value("${analysis-worker.ai-server-url}") String aiServerUrl,
            @Value("${internal.callback.token}") String internalToken
    ) {
        // uvicorn이 HTTP/2 upgrade 요청을 거절하면서 본문이 유실된다(FastApiAnalysisClient와
        // 같은 문제). HTTP/1.1로 고정한다.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(requestFactory)
                .build();
        this.internalToken = internalToken;
    }

    /** 실패는 RestClientException으로 올라간다 — 분류(4xx/5xx·타임아웃)는 호출자 몫. */
    public DocumentResponse generate(DocumentRequest request) {
        return restClient.post()
                .uri("/internal/v1/documents")
                .header(HEADER_INTERNAL_TOKEN, internalToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(DocumentResponse.class);
    }

    // ── AI 서버 요청/응답 스키마 (ai/ai_main/app/schemas.py의 DocumentRequest/
    // DocumentImage/DocumentResponse 기준, camelCase) ──
    //
    // documentRequestId는 보내지 않는다 — AI 스키마에 없는 필드다. 멱등키는
    // worker↔api 사이의 계약이고, AI는 요청·응답 어느 쪽에도 관여하지 않는다.
    // 이미지 순서는 payload 순서 그대로다(첫 번째가 기본 자료라는 전제. AI 스키마
    // 주석상 "순서는 참고용"이지만 그대로 전달해 모델이 참고하게 한다).

    public record DocumentRequest(
            Integer userId,
            List<SourceImage> images,
            String title,          // null이면 모델이 제목을 정한다
            String instruction,
            String language        // null 허용
    ) {}

    /** AI 스키마 필드명은 tags/category다(payload의 tagNames/categoryName과 다름 주의). */
    public record SourceImage(
            Integer imageId,
            String title,
            String summary,
            List<String> tags,
            String category,
            List<String> keyInformation,
            Ocr ocr,               // analysis_result가 없는 이미지는 null — AI가 skipped 처리
            String createdAt
    ) {}

    /**
     * AI의 OcrInput은 rawText/refinedText를 구분하고 refinedText가 있으면 그쪽을
     * 우선 쓴다. 우리가 보내는 값은 analysis_result.ocr_refined_text라 refinedText에
     * 싣는 게 맞다(rawText에 실어도 동작은 하지만 의미가 어긋난다).
     */
    public record Ocr(String refinedText) {}

    /**
     * 응답에는 skipped(분석 이력이 없어 건너뛴 이미지)도 오지만 worker는 쓰지 않는다.
     * summary·sections는 api-server가 문서 목록·상세를 구성하는 데 필요해 그대로 옮긴다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DocumentResponse(
            String title,
            String summary,
            String markdown,
            List<Section> sections,
            List<Integer> sourceImageIds,
            String modelVersion,
            String generatedAt
    ) {}

    /**
     * AI가 내려주는 본문 단락. 필드명은 AI 스키마 그대로다(heading/body) —
     * 명세 6-3의 이름(contentTitle/contentText)으로 바꾸는 건 DocumentGenerationService의
     * 매핑이 담당한다. sequence는 AI가 주지 않아 여기에도 없다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Section(
            String heading,
            String body,
            List<String> bullets,
            List<Integer> imageIds
    ) {}
}
