package com.ssafy.modera.worker.domain.analysis.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 외부 AI 서버(FastAPI) 호출 구현. AI_SERVER_URL 환경변수로 주소를 받는다.
 * AI 서버 API 스펙이 아직 확정되지 않아 요청/응답 DTO는 최소 형태의 TODO다.
 */
@Component
@ConditionalOnProperty(prefix = "analysis", name = "client", havingValue = "fastapi")
public class FastApiAnalysisClient implements AnalysisClient {

    private final RestClient restClient;

    public FastApiAnalysisClient(@Value("${analysis-worker.ai-server-url}") String aiServerUrl) {
        this.restClient = RestClient.builder().baseUrl(aiServerUrl).build();
    }

    @Override
    public AnalysisOutcome analyze(AnalysisRequest request) {
        // TODO: 경로("/analyze")와 요청/응답 스키마는 AI 서버 스펙 확정 후 교체한다.
        FastApiResponse response = restClient.post()
                .uri("/analyze")
                .body(new FastApiRequest(request.imageId(), request.s3Key()))
                .retrieve()
                .body(FastApiResponse.class);

        if (response == null) {
            throw new IllegalStateException("AI 서버 응답이 비어있다: imageId=" + request.imageId());
        }

        return new AnalysisOutcome(
                response.ocrRawText(),
                response.ocrRefinedText(),
                response.ocrLang(),
                response.ocrConfidence(),
                response.summary(),
                response.informative(),
                response.structuredType(),
                response.structuredFieldsJson(),
                response.keyInformationJson(),
                response.analysisConfidence(),
                response.embedding(),
                response.modelVersion()
        );
    }

    // TODO: AI 서버 스펙 확정 전까지의 임시 요청 형태
    private record FastApiRequest(String imageId, String s3Key) {
    }

    // TODO: AI 서버 스펙 확정 전까지의 임시 응답 형태
    private record FastApiResponse(
            String ocrRawText,
            String ocrRefinedText,
            String ocrLang,
            Float ocrConfidence,
            String summary,
            Boolean informative,
            String structuredType,
            String structuredFieldsJson,
            String keyInformationJson,
            Float analysisConfidence,
            float[] embedding,
            String modelVersion
    ) {
    }
}
