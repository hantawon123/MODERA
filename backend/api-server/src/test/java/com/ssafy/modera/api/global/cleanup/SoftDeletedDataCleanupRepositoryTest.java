package com.ssafy.modera.api.global.cleanup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

@ExtendWith(MockitoExtension.class)
class SoftDeletedDataCleanupRepositoryTest {

    @Mock JdbcTemplate jdbcTemplate;

    @Test
    void targetsOnlyLibraryAndQuerySchemasInChildFirstCleanup() {
        SoftDeletedDataCleanupRepository repository =
                new SoftDeletedDataCleanupRepository(jdbcTemplate);

        SoftDeletedDataCleanupResult result = repository.deleteBatch(1000);

        String executedSql = mockingDetails(jdbcTemplate).getInvocations().stream()
                .map(invocation -> invocation.getArgument(0, String.class))
                .reduce("", (left, right) -> left + "\n" + right);
        assertThat(executedSql)
                .contains("query_schema.document_image_view")
                .contains("query_schema.user_image_view")
                .contains("library_schema.user_image_category_history")
                .contains("library_schema.user_image")
                .doesNotContain("DELETE FROM image_schema")
                .doesNotContain("DELETE FROM document_schema")
                .doesNotContain("DELETE FROM schedule_schema");
        assertThat(result.deletedCount()).isZero();
        assertThat(result.mayHaveMore()).isFalse();
    }
}
