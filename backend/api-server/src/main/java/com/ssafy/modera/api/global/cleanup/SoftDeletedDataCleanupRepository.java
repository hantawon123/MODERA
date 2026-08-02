package com.ssafy.modera.api.global.cleanup;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SoftDeletedDataCleanupRepository {

    private static final List<String> CHILD_FIRST_TABLES = List.of(
            "query_schema.document_image_view",
            "query_schema.user_document_view",
            "query_schema.user_schedule_view",
            "query_schema.user_category_view",
            "query_schema.user_image_view",
            "library_schema.user_image_category_history",
            "library_schema.image_document",
            "library_schema.image_schedule",
            "library_schema.user_favorite_image",
            "library_schema.image_category",
            "library_schema.image_tag",
            "library_schema.user_document",
            "library_schema.user_schedule"
    );

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public SoftDeletedDataCleanupResult deleteBatch(int batchSize) {
        markHistoryOfDeletedUserImages();

        int totalDeleted = 0;
        boolean mayHaveMore = false;
        for (String table : CHILD_FIRST_TABLES) {
            int deleted = deleteFromTable(table, batchSize);
            totalDeleted += deleted;
            mayHaveMore |= deleted == batchSize;
        }

        int deletedUserImages = deleteUserImagesWithoutHistory(batchSize);
        totalDeleted += deletedUserImages;
        mayHaveMore |= deletedUserImages == batchSize;
        return new SoftDeletedDataCleanupResult(totalDeleted, mayHaveMore);
    }

    private void markHistoryOfDeletedUserImages() {
        jdbcTemplate.update(
                """
                UPDATE library_schema.user_image_category_history history
                SET del_yn = 'Y'
                FROM library_schema.user_image user_image
                WHERE history.user_image_id = user_image.user_image_id
                  AND user_image.del_yn = 'Y'
                  AND history.del_yn = 'N'
                """
        );
    }

    private int deleteFromTable(String table, int batchSize) {
        return jdbcTemplate.update(
                """
                DELETE FROM %s
                WHERE ctid IN (
                    SELECT ctid
                    FROM %s
                    WHERE del_yn = 'Y'
                    LIMIT ?
                )
                """.formatted(table, table),
                batchSize
        );
    }

    private int deleteUserImagesWithoutHistory(int batchSize) {
        return jdbcTemplate.update(
                """
                DELETE FROM library_schema.user_image user_image
                WHERE user_image.ctid IN (
                    SELECT candidate.ctid
                    FROM library_schema.user_image candidate
                    WHERE candidate.del_yn = 'Y'
                      AND NOT EXISTS (
                          SELECT 1
                          FROM library_schema.user_image_category_history history
                          WHERE history.user_image_id = candidate.user_image_id
                      )
                    LIMIT ?
                )
                """,
                batchSize
        );
    }
}
