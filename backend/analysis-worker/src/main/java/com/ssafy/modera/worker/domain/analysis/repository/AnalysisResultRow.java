package com.ssafy.modera.worker.domain.analysis.repository;

import java.time.Instant;
import java.util.UUID;

public record AnalysisResultRow(
        Long jobId,
        UUID imageId,
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
