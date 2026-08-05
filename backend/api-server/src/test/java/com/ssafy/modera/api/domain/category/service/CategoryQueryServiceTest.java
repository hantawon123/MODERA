package com.ssafy.modera.api.domain.category.service;

import com.ssafy.modera.api.domain.category.repository.CategoryListRow;
import com.ssafy.modera.api.domain.category.repository.CategoryQueryRepository;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceTest {

    @Mock CategoryQueryRepository categoryQueryRepository;
    @Mock CategoryImageUrlFactory categoryImageUrlFactory;
    @InjectMocks CategoryQueryService categoryQueryService;

    @Test
    void returnsCategoryPageInSpecificationShape() {
        OffsetDateTime latestUploadedAt = OffsetDateTime.now();
        when(categoryQueryRepository.findCategories(
                1, "category_name ASC, category_id ASC"))
                .thenReturn(List.of(new CategoryListRow(
                        3, "공부", 42, latestUploadedAt)));

        var result = categoryQueryService.getCategories(1, " name_asc ");

        assertThat(result.list().getFirst().categoryId()).isEqualTo(3);
        assertThat(result.list().getFirst().name()).isEqualTo("공부");
        // 목록에는 presigned URL이 아니라 불변 경로가 나간다(앱 Room 저장·캐시 키용).
        assertThat(result.list().getFirst().categoryImageUrl())
                .isEqualTo("/api/v1/categories/3/thumbnail");
        assertThat(result.list().getFirst().imageCount()).isEqualTo(42);
        assertThat(result.list().getFirst().latestUpdatedAt()).isEqualTo(latestUploadedAt);
    }

    @Test
    void redirectsThumbnailToPresignedUrlOnlyForOwnedCategory() {
        when(categoryQueryRepository.existsCategory(1, 1744084819)).thenReturn(true);
        when(categoryImageUrlFactory.createViewUrl(1744084819))
                .thenReturn("http://localhost/presigned");

        assertThat(categoryQueryService.getThumbnailRedirectUrl(1, 1744084819))
                .isEqualTo("http://localhost/presigned");
    }

    @Test
    void hidesUnownedCategoryThumbnailAsNotFound() {
        when(categoryQueryRepository.existsCategory(1, 99)).thenReturn(false);

        assertThatThrownBy(() -> categoryQueryService.getThumbnailRedirectUrl(1, 99))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void rejectsUnsupportedSortIncludingOldCommaStyle() {
        assertThatThrownBy(() ->
                categoryQueryService.getCategories(1, "NAME_DESC"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() ->
                categoryQueryService.getCategories(1, "name,asc"))
                .isInstanceOf(BusinessException.class);
    }
}
