package com.ssafy.modera.worker.domain.analysis.client;

public record AnalysisOutcome(
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
