package com.ssafy.modera.api.domain.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserDataResetRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean existsActiveUser(Integer userId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM user_schema.users
                    WHERE user_id = ?
                      AND del_yn = 'N'
                )
                """,
                Boolean.class,
                userId
        ));
    }

    public void softDeleteAll(Integer userId) {
        softDeleteCategoryHistory(userId);
        softDeleteDocumentRelations(userId);
        softDeleteScheduleRelations(userId);
        softDeleteDirectLibraryRelations(userId);
        softDeleteQueryModels(userId);
        softDeleteRequestHistory(userId);
    }

    private void softDeleteCategoryHistory(Integer userId) {
        jdbcTemplate.update(
                """
                UPDATE library_schema.user_image_category_history history
                SET del_yn = 'Y'
                FROM library_schema.user_image user_image
                WHERE history.user_image_id = user_image.user_image_id
                  AND user_image.user_id = ?
                  AND history.del_yn = 'N'
                """,
                userId
        );
    }

    private void softDeleteDocumentRelations(Integer userId) {
        jdbcTemplate.update(
                """
                UPDATE library_schema.image_document image_document
                SET del_yn = 'Y'
                WHERE image_document.del_yn = 'N'
                  AND EXISTS (
                      SELECT 1
                      FROM library_schema.user_document user_document
                      WHERE user_document.user_id = ?
                        AND user_document.document_id = image_document.document_id
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM library_schema.user_document other_user_document
                      WHERE other_user_document.user_id <> ?
                        AND other_user_document.document_id = image_document.document_id
                        AND other_user_document.del_yn = 'N'
                  )
                """,
                userId,
                userId
        );
    }

    private void softDeleteScheduleRelations(Integer userId) {
        jdbcTemplate.update(
                """
                UPDATE library_schema.image_schedule image_schedule
                SET del_yn = 'Y'
                WHERE image_schedule.del_yn = 'N'
                  AND EXISTS (
                      SELECT 1
                      FROM library_schema.user_schedule user_schedule
                      WHERE user_schedule.user_id = ?
                        AND user_schedule.schedule_id = image_schedule.schedule_id
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM library_schema.user_schedule other_user_schedule
                      WHERE other_user_schedule.user_id <> ?
                        AND other_user_schedule.schedule_id = image_schedule.schedule_id
                        AND other_user_schedule.del_yn = 'N'
                  )
                """,
                userId,
                userId
        );
    }

    private void softDeleteDirectLibraryRelations(Integer userId) {
        jdbcTemplate.update(
                """
                UPDATE library_schema.user_favorite_image
                SET del_yn = 'Y'
                WHERE user_id = ? AND del_yn = 'N'
                """,
                userId
        );
        jdbcTemplate.update(
                """
                UPDATE library_schema.user_document
                SET del_yn = 'Y'
                WHERE user_id = ? AND del_yn = 'N'
                """,
                userId
        );
        jdbcTemplate.update(
                """
                UPDATE library_schema.user_schedule
                SET del_yn = 'Y'
                WHERE user_id = ? AND del_yn = 'N'
                """,
                userId
        );
        jdbcTemplate.update(
                """
                UPDATE library_schema.user_image
                SET del_yn = 'Y',
                    pending_category_request_id = NULL
                WHERE user_id = ? AND del_yn = 'N'
                """,
                userId
        );
    }

    private void softDeleteQueryModels(Integer userId) {
        jdbcTemplate.update(
                """
                UPDATE query_schema.document_image_view
                SET del_yn = 'Y'
                WHERE user_id = ? AND del_yn = 'N'
                """,
                userId
        );
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_document_view
                SET del_yn = 'Y'
                WHERE user_id = ? AND del_yn = 'N'
                """,
                userId
        );
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_schedule_view
                SET del_yn = 'Y'
                WHERE user_id = ? AND del_yn = 'N'
                """,
                userId
        );
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_category_view
                SET del_yn = 'Y'
                WHERE user_id = ? AND del_yn = 'N'
                """,
                userId
        );
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_image_view
                SET del_yn = 'Y'
                WHERE user_id = ? AND del_yn = 'N'
                """,
                userId
        );
    }

    private void softDeleteRequestHistory(Integer userId) {
        jdbcTemplate.update(
                """
                UPDATE image_schema.image_registration_request
                SET del_yn = 'Y'
                WHERE user_id = ? AND del_yn = 'N'
                """,
                userId
        );
        jdbcTemplate.update(
                """
                UPDATE document_schema.document_generation_request
                SET del_yn = 'Y'
                WHERE user_id = ? AND del_yn = 'N'
                """,
                userId
        );
    }
}
