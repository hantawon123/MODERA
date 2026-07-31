package com.ssafy.modera.api.domain.category.repository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryCommandRepositoryTest {

    @Test
    void includesCurrentCategoryWhenInitialHistoryIsMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        CategoryCommandRepository repository =
                new CategoryCommandRepository(jdbcTemplate);
        UUID requestId = UUID.randomUUID();
        ArgumentCaptor<String> excludedSql = ArgumentCaptor.forClass(String.class);

        when(jdbcTemplate.query(
                any(String.class),
                any(org.springframework.jdbc.core.RowMapper.class),
                eq(7),
                eq(18)
        )).thenReturn(List.of(31));
        when(jdbcTemplate.queryForList(
                any(String.class), eq(Integer.class), eq(31), eq(31)
        )).thenReturn(List.of(3));
        when(jdbcTemplate.update(
                any(String.class), eq(requestId), eq(31)
        )).thenReturn(1);

        CategoryReanalysisTarget target =
                repository.prepareRequest(7, 18, requestId).orElseThrow();

        assertThat(target.excludedCategoryIds()).containsExactly(3);
        verify(jdbcTemplate).queryForList(
                excludedSql.capture(), eq(Integer.class), eq(31), eq(31));
        assertThat(excludedSql.getValue())
                .contains("current_category_id")
                .contains("view.category_id")
                .contains("category.category_id")
                .contains("DISTINCT ON (category_id)")
                .contains("LIMIT 5");
    }

    @Test
    void refusesToPublishReanalysisWithoutAnyResolvableCategoryId() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        CategoryCommandRepository repository =
                new CategoryCommandRepository(jdbcTemplate);
        UUID requestId = UUID.randomUUID();

        when(jdbcTemplate.query(
                any(String.class),
                any(org.springframework.jdbc.core.RowMapper.class),
                eq(7),
                eq(18)
        )).thenReturn(List.of(31));
        when(jdbcTemplate.queryForList(
                any(String.class), eq(Integer.class), eq(31), eq(31)
        )).thenReturn(List.of());

        assertThat(repository.prepareRequest(7, 18, requestId)).isEmpty();
        verify(jdbcTemplate, org.mockito.Mockito.never()).update(
                any(String.class), any(), any());
    }
}
