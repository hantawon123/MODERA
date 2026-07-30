package com.ssafy.modera.api.domain.category.service;

import com.ssafy.modera.api.domain.category.repository.CategoryListRow;
import com.ssafy.modera.api.domain.category.repository.CategoryQueryRepository;
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
                        3, "공부", "categories/study.jpg", 42, latestUploadedAt)));
        when(categoryImageUrlFactory.createViewUrl("categories/study.jpg"))
                .thenReturn("http://localhost/category-image");

        var result = categoryQueryService.getCategories(1, "name,asc");

        assertThat(result.list().getFirst().categoryId()).isEqualTo(3);
        assertThat(result.list().getFirst().name()).isEqualTo("공부");
        assertThat(result.list().getFirst().categoryImageUrl())
                .isEqualTo("http://localhost/category-image");
        assertThat(result.list().getFirst().imageCount()).isEqualTo(42);
        assertThat(result.list().getFirst().latestUpdatedAt()).isEqualTo(latestUploadedAt);
    }

    @Test
    void rejectsUnsupportedSortAndInvalidPage() {
        assertThatThrownBy(() ->
                categoryQueryService.getCategories(1, "name,desc"))
                .isInstanceOf(BusinessException.class);
    }
}
