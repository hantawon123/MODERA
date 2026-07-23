package com.ssafy.modera.worker.domain.analysis.client;

public record AnalysisRequest(Integer imageId, String s3Key) {
}
