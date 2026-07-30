package com.ssafy.modera.api.domain.image.dto.response;

import java.util.List;

public record ImageDeleteResponse(
        List<Integer> deletedImageIds,
        List<Integer> alreadyDeletedImageIds,
        List<Failed> failed,
        int deletedCount,
        int failedCount
) {
    public record Failed(
            Integer imageId,
            String reason
    ) {
    }
}
