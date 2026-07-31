package com.ssafy.modera.api.domain.category.service;

import com.ssafy.modera.api.domain.category.dto.response.CategoryListResponse;
import com.ssafy.modera.api.domain.category.dto.response.CategorySummaryResponse;
import com.ssafy.modera.api.domain.category.repository.CategoryListRow;
import com.ssafy.modera.api.domain.category.repository.CategoryQueryRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryService {

    // 다른 목록 API(5-1 TITLE_ASC, 8-1 UPDATED_DESC, 9-1 START_ASC)와 같은 enum 방식.
    private static final Map<String, String> SORT_SQL = Map.of(
            "NAME_ASC", "category_name ASC, category_id ASC",
            "UPDATED_DESC", "latest_uploaded_at DESC NULLS LAST, category_id ASC",
            "IMAGE_COUNT_DESC", "image_count DESC, category_id ASC"
    );

    private final CategoryQueryRepository categoryQueryRepository;
    private final CategoryImageUrlFactory categoryImageUrlFactory;

    public CategoryListResponse getCategories(Integer userId, String sort) {
        String normalizedSort = sort == null || sort.isBlank()
                ? "NAME_ASC"
                : sort.trim().toUpperCase(Locale.ROOT);
        String orderBy = SORT_SQL.get(normalizedSort);
        if (orderBy == null) {
            throw new BusinessException(GlobalErrorCode.INVALID_PARAMETER);
        }

        var list = categoryQueryRepository.findCategories(userId, orderBy).stream()
                .map(this::toResponse)
                .toList();
        return new CategoryListResponse(list);
    }

    private CategorySummaryResponse toResponse(CategoryListRow row) {
        return new CategorySummaryResponse(
                row.categoryId(),
                row.name(),
                categoryImageUrlFactory.createViewUrl(row.imageS3Key()),
                row.imageCount(),
                row.latestUploadedAt()
        );
    }
}
