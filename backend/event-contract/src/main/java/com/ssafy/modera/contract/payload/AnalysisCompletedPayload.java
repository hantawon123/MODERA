package com.ssafy.modera.contract.payload;

import java.util.List;

/**
 * {@link com.ssafy.modera.contract.EventTypes#ANALYSIS_COMPLETED} 이벤트 payload.
 */
public record AnalysisCompletedPayload(
        int imageId,
        int userId,
        String summary,
        String ocrText,
        String categoryName,
        List<String> tagNames,
        String structuredFields,
        String analysisStatus,
        String modelVersion
) {
}
