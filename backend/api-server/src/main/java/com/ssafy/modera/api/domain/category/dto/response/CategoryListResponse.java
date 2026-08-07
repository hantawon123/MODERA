package com.ssafy.modera.api.domain.category.dto.response;

import java.util.List;

public record CategoryListResponse(
        List<CategorySummaryResponse> list
) {
}
