package com.ssafy.modera.worker.domain.analysis.client;

public interface AnalysisClient {

    AnalysisOutcome analyze(AnalysisRequest request);
}
