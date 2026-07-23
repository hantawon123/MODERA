package com.ssafy.modera.worker.domain.analysis.repository;

import java.time.Instant;

public record AnalysisResultRow(
        Long jobId,
        Integer imageId,
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
        String modelVersion,
        Instant analyzedAt
) {
}
