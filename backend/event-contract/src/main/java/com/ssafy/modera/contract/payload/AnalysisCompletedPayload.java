package com.ssafy.modera.contract.payload;

import java.util.List;

/**
 * {@link com.ssafy.modera.contract.EventTypes#ANALYSIS_COMPLETED} 이벤트 payload.
 */
public record AnalysisCompletedPayload(
        int imageId,
        int userId,
        String title,
        String summary,
        String ocrText,
        String thumbnailKey,
        String categoryName,
        List<String> tagNames,
        List<String> keyInformation,
        String structuredType,
        String structuredFields,
        String analysisStatus,
        String modelVersion,
        String triggerType
) {
}
