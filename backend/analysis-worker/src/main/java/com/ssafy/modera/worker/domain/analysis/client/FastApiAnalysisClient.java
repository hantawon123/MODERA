package com.ssafy.modera.worker.domain.analysis.client;

import com.ssafy.modera.contract.payload.ImageUploadedPayload;
import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import com.ssafy.modera.worker.domain.analysis.AnalysisPerformanceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * 외부 AI 서버(FastAPI) 호출 구현.
 * AI는 요청을 202로 접수만 하고, 실제 결과는 callbackUrl로 콜백한다.
 * 이 클래스는 요청 전송까지만 책임
 * 결과는 다루지 않는다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "analysis", name = "client", havingValue = "fastapi")
public class FastApiAnalysisClient implements AnalysisClient {

    // AI 서버가 한 요청에서 전체 파이프라인 (LLM -> IMAGE_ANALysIS -> AGENT)을 처리하는 stage
    private static final String STAGE_FULL = "FULL";
    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    /** 이미지당 AI가 추출하는 태그 상한. 10개는 목록 카드에 다 못 보여줘 5개로 줄였다. */
    private static final int MAX_TAGS = 5;

    private final RestClient restClient;
    private final String internalToken;
    private final String callbackUrl;
    private final AnalysisPerformanceMetrics performanceMetrics;

    public FastApiAnalysisClient(
            @Value("${analysis-worker.ai-server-url}") String aiServerUrl,
            @Value("${internal.callback.token}") String internalToken,
            @Value("${analysis.callback-url}") String callbackUrl,
            AnalysisPerformanceMetrics performanceMetrics
    ) {
        // uvicorn이 HTTP/2 upgrade 요청을 거절하면서 본문이 유실된다("Unsupported upgrade request").
        // HTTP/1.1로 고정한다.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        this.restClient = RestClient.builder()
                .baseUrl(aiServerUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        this.internalToken = internalToken;
        this.callbackUrl = callbackUrl;
        this.performanceMetrics = performanceMetrics;
    }

    @Override
    public void requestAnalysis(AnalysisJob job, String s3Key, ImageUploadedPayload.ClientOcr clientOcr) {
        AnalyzeRequest body = new AnalyzeRequest(
                job.getJobId(),
                job.getImageId(),
                job.getUserId(),
                STAGE_FULL,
                new AnalyzeInput(new ImageInput(s3Key), toOcrInput(clientOcr)),
                new AnalyzeOptions(MAX_TAGS, "ko"),
                callbackUrl
        );

        long started = System.nanoTime();
        AnalyzeAccepted accepted;
        try {
            accepted = restClient.post()
                    .uri("/internal/v1/analyze")
                    .header(HEADER_INTERNAL_TOKEN, internalToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(AnalyzeAccepted.class);
            performanceMetrics.recordAiRequest(System.nanoTime() - started, "SUCCESS");
        } catch (RuntimeException exception) {
            performanceMetrics.recordAiRequest(System.nanoTime() - started, "FAILED");
            throw exception;
        }

        // 같은 (jobId, stage)를 다시 보내면 AI가 200 + error=DUPLICATE_JOB으로 돌려준다.
        // 이 경우 이미 처리 중이거나 완료된 작업이므로 정상으로 간주하고 콜백을 기다린다.
        if (accepted != null && accepted.error() != null) {
            log.info("AI가 중복 작업으로 응답 - 기존 처리 대기: jobId={} error={}",
                    job.getJobId(), accepted.error());
            return;
        }
        log.info("AI 분석 요청 접수됨: jobId={} imageId={} stage={} ocr={}",
                job.getJobId(), job.getImageId(), STAGE_FULL, toOcrInput(clientOcr) != null);
    }

    private OcrInput toOcrInput(ImageUploadedPayload.ClientOcr clientOcr) {
        if (clientOcr == null || clientOcr.rawText() == null || clientOcr.rawText().isBlank()) {
            return null;
        }
        return new OcrInput(clientOcr.rawText(), clientOcr.lang(), clientOcr.confidence());
    }


    // ── AI 서버 요청/응답 스키마 (ai/ai_main/app/schemas.py 기준) ──
    private record AnalyzeRequest(
            Integer jobId,
            Integer imageId,
            Integer userId,
            String stage,
            AnalyzeInput input,
            AnalyzeOptions options,
            String callbackUrl
    ) {}

    private record AnalyzeInput(ImageInput image, OcrInput ocr) {}

    private record ImageInput(String s3Key) {}

    private record OcrInput(String rawText, String lang, Double confidence) {}

    private record AnalyzeOptions(Integer maxTags, String language) {}

    /** AI의 접수 확인 응답(202).
     * 결과가 아니라 "받았다"는 신호다. */
    private record AnalyzeAccepted(
            Integer jobId,
            Integer imageId,
            String stage,
            Boolean accepted,
            String status,
            String error      // DUPLICATE_JOB 등, 정상 접수 시 null
    ) {}
}
