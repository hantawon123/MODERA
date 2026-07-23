package com.ssafy.modera.worker.domain.analysis.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * AI 서버 없이 이벤트 파이프라인을 검증하기 위한 목 구현. local 기본값.
 */
@Component
@ConditionalOnProperty(prefix = "analysis", name = "client", havingValue = "mock", matchIfMissing = true)
public class MockAnalysisClient implements AnalysisClient {

    private static final String MODEL_VERSION = "mock-1.0";

    @Override
    public AnalysisOutcome analyze(AnalysisRequest request) {
        return new AnalysisOutcome(
                "MOCK RAW OCR TEXT",
                "MOCK REFINED OCR TEXT",
                "ko",
                0.95f,
                "MOCK 분석 요약입니다.",
                true,
                null,
                null,
                null,
                0.9f,
                randomEmbedding(768),
                MODEL_VERSION
        );
    }

    private float[] randomEmbedding(int dimension) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float[] embedding = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            embedding[i] = random.nextFloat() * 2f - 1f;
        }
        return embedding;
    }
}
