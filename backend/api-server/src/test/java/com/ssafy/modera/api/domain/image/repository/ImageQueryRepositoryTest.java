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

class ImageQueryRepositoryTest {

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
