package com.ssafy.modera.api.domain.image.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageQueryRepositoryTest {

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
                .contains("ON CONFLICT (user_id, category_id) DO UPDATE");
        assertThat(sqlCaptor.getAllValues().get(1))
                .contains("UPDATE query_schema.user_category_view")
                .contains("image_count")
                .contains("latest_uploaded_at");
    }
}
