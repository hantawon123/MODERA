package com.ssafy.modera.worker.domain.analysis.client;

import com.ssafy.modera.worker.domain.analysis.callback.dto.request.AnalysisCallbackRequest;
import com.ssafy.modera.worker.domain.analysis.callback.service.AnalysisCallbackService;
import com.ssafy.modera.worker.domain.analysis.entity.AnalysisJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI 서버 없이 파이프라인을 검증하기 위한 목 구현. local 기본값.
 * 실제 AI처럼 HTTP 콜백을 쏘는 대신, 콜백 처리 서비스를 직접 호출해 같은 경로를 태운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "analysis", name = "client", havingValue = "mock", matchIfMissing = true)
public class MockAnalysisClient implements AnalysisClient {

    private static final String MODEL_VERSION = "mock-1.0";
    private static final int EMBEDDING_DIM = 768;

    private final AnalysisCallbackService analysisCallbackService;

    @Override
    public void requestAnalysis(AnalysisJob job, String s3Key) {
        log.info("[mock] 분석 요청 - 즉시 콜백 처리: jobId={} imageId={}", job.getJobId(), job.getImageId());

        AnalysisCallbackRequest callback = new AnalysisCallbackRequest(
                job.getJobId(),
                job.getImageId(),
                "FULL",
                "COMPLETED",
                Map.of(
                        "summary", "MOCK 분석 요약입니다.",
                        "ocrRefinedText", "MOCK REFINED OCR TEXT",
                        "analysisConfidence", 0.9,
                        "documentVector", randomEmbedding()
                ),
                null,
                MODEL_VERSION,
                OffsetDateTime.now().toString()
        );

        analysisCallbackService.handle(callback);
    }

    private List<Double> randomEmbedding() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Double> embedding = new ArrayList<>(EMBEDDING_DIM);
        for (int i = 0; i < EMBEDDING_DIM; i++) {
            embedding.add(random.nextDouble() * 2d - 1d);
        }
        return embedding;
    }
}