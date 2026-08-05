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
        // 만료·인증 없는 공개 URL — 앱이 Room에 저장하고 헤더 없이 바로 로드한다.
        when(categoryImageUrlFactory.createPublicUrl(3))
                .thenReturn("https://storage.example/category-thumbnails/3.png");

        var result = categoryQueryService.getCategories(1, " name_asc ");

        assertThat(result.list().getFirst().categoryId()).isEqualTo(3);
        assertThat(result.list().getFirst().name()).isEqualTo("공부");
        assertThat(result.list().getFirst().categoryImageUrl())
                .isEqualTo("https://storage.example/category-thumbnails/3.png");
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
