package com.ssafy.modera.api.domain.user.repository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDataResetRepositoryTest {

    @Test
    void checksOnlyActiveAccount() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        UserDataResetRepository repository =
                new UserDataResetRepository(jdbcTemplate);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        when(jdbcTemplate.queryForObject(
                any(String.class), eq(Boolean.class), eq(2)))
                .thenReturn(true);

        assertThat(repository.existsActiveUser(2)).isTrue();

        verify(jdbcTemplate).queryForObject(
                sql.capture(), eq(Boolean.class), eq(2));
        assertThat(sql.getValue())
                .contains("user_schema.users")
                .contains("del_yn = 'N'");
    }

    @Test
    void softDeletesEveryUserOwnedRelationReadModelAndRequestHistory() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        UserDataResetRepository repository =
                new UserDataResetRepository(jdbcTemplate);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);

        repository.softDeleteAll(2);

        verify(jdbcTemplate, times(12)).update(sql.capture(), eq(2));
        verify(jdbcTemplate, times(2)).update(sql.capture(), eq(2), eq(2));
        String allSql = String.join("\n", List.copyOf(sql.getAllValues()));
        assertThat(allSql)
                .contains("user_image_category_history")
                .contains("image_document")
                .contains("image_schedule")
                .contains("user_favorite_image")
                .contains("user_document")
                .contains("user_schedule")
                .contains("user_image")
                .contains("document_image_view")
                .contains("user_document_view")
                .contains("user_schedule_view")
                .contains("user_category_view")
                .contains("user_image_view")
                .contains("image_registration_request")
                .contains("document_generation_request");
    }
}
