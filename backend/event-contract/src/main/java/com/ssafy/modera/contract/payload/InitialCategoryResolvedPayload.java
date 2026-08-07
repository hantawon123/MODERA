package com.ssafy.modera.contract.payload;

public record InitialCategoryResolvedPayload(
        int imageId,
        int userId,
        int categoryId,
        String categoryName,
        String triggerType
) {
}
