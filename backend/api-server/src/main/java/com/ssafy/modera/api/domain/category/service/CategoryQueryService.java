package com.ssafy.modera.api.domain.category.service;

import com.ssafy.modera.api.domain.category.dto.response.CategoryListResponse;
import com.ssafy.modera.api.domain.category.dto.response.CategorySummaryResponse;
import com.ssafy.modera.api.domain.category.repository.CategoryListRow;
import com.ssafy.modera.api.domain.category.repository.CategoryQueryRepository;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
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

    /** 목록 응답에 싣는 아이콘 고정 경로. 실제 이미지는 이 경로의 302 리다이렉트가 준다. */
    private static final String THUMBNAIL_PATH_FORMAT = "/api/v1/categories/%d/thumbnail";

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
                // presigned URL이 아니라 불변 경로다. 앱이 Room에 영구 저장하고 이미지
                // 캐시 키로도 쓴다 — presign은 이 경로의 6-2 리다이렉트가 그때그때 한다.
                THUMBNAIL_PATH_FORMAT.formatted(row.categoryId()),
                row.imageCount(),
                row.latestUploadedAt()
        );
    }

    /**
     * 카테고리 아이콘 리다이렉트(6-2)가 보낼 presigned URL. 소유권을 먼저 확인한다 —
     * 남의 카테고리는 존재 여부를 숨기고 404다.
     *
     * <p>아이콘 파일은 AI 서버가 카테고리 판정 시 백그라운드로 생성해
     * category-thumbnails/{categoryId}.png로 올려둔다. 키가 결정적이라 존재 확인 없이
     * 서명만 한다 — 생성이 끝나기 전에는 이 URL이 404를 반환하고, 앱이 플레이스홀더로
     * 처리한 뒤 다음 로드에서 자연 복구된다.
     */
    public String getThumbnailRedirectUrl(Integer userId, Integer categoryId) {
        if (!categoryQueryRepository.existsCategory(userId, categoryId)) {
            throw new BusinessException(ImageErrorCode.CATEGORY_NOT_FOUND);
        }
        return categoryImageUrlFactory.createViewUrl(categoryId);
    }
}
