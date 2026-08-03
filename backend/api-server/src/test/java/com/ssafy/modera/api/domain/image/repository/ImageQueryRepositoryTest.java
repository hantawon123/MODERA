package com.ssafy.modera.api.domain.image.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageQueryRepositoryTest {

    @Test
    @SuppressWarnings("unchecked")
    void usesCategoryReadModelCountForUnfilteredCategoryList() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ImageQueryRepository repository =
                new ImageQueryRepository(jdbcTemplate, new ObjectMapper());
        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);

        when(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), eq(7), eq(3)
        )).thenReturn(135L);
        when(jdbcTemplate.query(
                anyString(), any(RowMapper.class), any(Object[].class)
        )).thenReturn(List.of());

        ImageListPage result = repository.findImages(
                7, null, 3, null, "UPLOADED_DESC", 0, 20);

        verify(jdbcTemplate).queryForObject(
                countSql.capture(), eq(Long.class), eq(7), eq(3));
        assertThat(countSql.getValue())
                .contains("query_schema.user_category_view")
                .contains("category_view.image_count")
                .doesNotContain("COUNT(*)");
        assertThat(result.totalElements()).isEqualTo(135L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesExactCountForFavoriteOrKeywordFilters() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ImageQueryRepository repository =
                new ImageQueryRepository(jdbcTemplate, new ObjectMapper());
        ArgumentCaptor<String> countSql = ArgumentCaptor.forClass(String.class);

        when(jdbcTemplate.queryForObject(
                anyString(), eq(Long.class), any(Object[].class)
        )).thenReturn(12L);
        when(jdbcTemplate.query(
                anyString(), any(RowMapper.class), any(Object[].class)
        )).thenReturn(List.of());

        ImageListPage result = repository.findImages(
                7, true, 3, "receipt", "UPLOADED_DESC", 0, 20);

        verify(jdbcTemplate).queryForObject(
                countSql.capture(), eq(Long.class), any(Object[].class));
        assertThat(countSql.getValue())
                .startsWith("SELECT COUNT(*)")
                .contains("image_view.favorite = ?")
                .contains("jsonb_array_elements(image_view.tags)");
        assertThat(result.totalElements()).isEqualTo(12L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sortsTitlesUsingKoreanDictionaryCollation() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ImageQueryRepository repository =
                new ImageQueryRepository(jdbcTemplate, new ObjectMapper());
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Long.class),
                any(Object[].class)
        )).thenReturn(0L);
        when(jdbcTemplate.query(
                anyString(),
                any(RowMapper.class),
                any(Object[].class)
        )).thenReturn(List.of());

        repository.findImages(7, null, null, null, "TITLE_ASC", 0, 20);

        verify(jdbcTemplate).query(
                sqlCaptor.capture(),
                any(RowMapper.class),
                any(Object[].class)
        );
        assertThat(sqlCaptor.getValue())
                .contains("LOWER(COALESCE(image_view.title, ''))")
                .contains("COLLATE \"ko-KR-x-icu\" ASC")
                .contains("image_view.image_id ASC");
    }

    @Test
    void reconnectsDeletedReadModelFromExistingCompletedAnalysis() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ImageQueryRepository repository =
                new ImageQueryRepository(jdbcTemplate, new ObjectMapper());
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        when(jdbcTemplate.update(
                org.mockito.ArgumentMatchers.anyString(),
                eq(7),
                eq(20),
                eq(7)
        )).thenReturn(1);

        boolean copied = repository.copyExistingView(7, 20);

        assertThat(copied).isTrue();
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq(7), eq(20), eq(7));
        assertThat(sqlCaptor.getValue())
                .contains("source.analysis_status IN ('COMPLETED', 'EMPTY')")
                .doesNotContain("AND source.del_yn = 'N'")
                .contains("del_yn = 'N'");
    }

    @Test
    void synchronizesNewAndExistingUserCategoryRows() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ImageQueryRepository repository =
                new ImageQueryRepository(jdbcTemplate, new ObjectMapper());
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        repository.synchronizeUserCategories(7);

        verify(jdbcTemplate, times(2)).update(sqlCaptor.capture(), eq(7));
        assertThat(sqlCaptor.getAllValues().get(0))
                .contains("INSERT INTO query_schema.user_category_view")
                .contains("analysis_status IN ('COMPLETED', 'EMPTY')")
                .doesNotContain("JOIN library_schema.user_image")
                .contains("ON CONFLICT (user_id, category_id) DO UPDATE");
        assertThat(sqlCaptor.getAllValues().get(1))
                .contains("UPDATE query_schema.user_category_view")
                .contains("image_count")
                .contains("latest_uploaded_at")
                .contains("analysis_status IN ('COMPLETED', 'EMPTY')")
                .doesNotContain("JOIN library_schema.user_image");
    }
}
