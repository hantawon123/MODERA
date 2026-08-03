package com.ssafy.modera.api.domain.image.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ImageCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Integer updateFavorite(Integer userId, Integer imageId, boolean favorite) {
        if (!hasUserImage(userId, imageId, "N")) {
            return null;
        }

        if (favorite) {
            jdbcTemplate.update(
                    """
                    INSERT INTO library_schema.user_favorite_image (
                        user_id, image_id, del_yn
                    )
                    VALUES (?, ?, 'N')
                    ON CONFLICT (user_id, image_id)
                    DO UPDATE SET del_yn = 'N'
                    """,
                    userId,
                    imageId
            );
        } else {
            jdbcTemplate.update(
                    """
                    UPDATE library_schema.user_favorite_image
                    SET del_yn = 'Y'
                    WHERE user_id = ?
                      AND image_id = ?
                      AND del_yn = 'N'
                    """,
                    userId,
                    imageId
            );
        }

        jdbcTemplate.update(
                """
                UPDATE query_schema.user_image_view
                SET favorite = ?
                WHERE user_id = ?
                  AND image_id = ?
                  AND del_yn = 'N'
                """,
                favorite,
                userId,
                imageId
        );

        Integer favoriteCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM query_schema.user_image_view image_view
                JOIN library_schema.user_image user_image
                  ON user_image.user_id = image_view.user_id
                 AND user_image.image_id = image_view.image_id
                 AND user_image.del_yn = 'N'
                WHERE image_view.user_id = ?
                  AND image_view.favorite = TRUE
                  AND image_view.del_yn = 'N'
                  AND image_view.analysis_status IN ('COMPLETED', 'EMPTY')
                """,
                Integer.class,
                userId
        );
        return favoriteCount == null ? 0 : favoriteCount;
    }

    @Transactional
    public ImageDeleteStatus deleteImage(Integer userId, Integer imageId) {
        if (!hasUserImage(userId, imageId, "N")) {
            return hasUserImage(userId, imageId, "Y")
                    ? ImageDeleteStatus.ALREADY_DELETED
                    : ImageDeleteStatus.NOT_FOUND;
        }

        List<Integer> documentIds = findActiveDocumentIds(userId, imageId);
        List<Integer> categoryIds = findActiveCategoryIds(userId, imageId);

        jdbcTemplate.update(
                """
                UPDATE library_schema.user_image
                SET del_yn = 'Y'
                WHERE user_id = ? AND image_id = ? AND del_yn = 'N'
                """,
                userId,
                imageId
        );
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_image_view
                SET del_yn = 'Y'
                WHERE user_id = ? AND image_id = ? AND del_yn = 'N'
                """,
                userId,
                imageId
        );
        jdbcTemplate.update(
                """
                UPDATE library_schema.user_favorite_image
                SET del_yn = 'Y'
                WHERE user_id = ? AND image_id = ? AND del_yn = 'N'
                """,
                userId,
                imageId
        );
        jdbcTemplate.update(
                """
                UPDATE library_schema.user_image_category_history history
                SET del_yn = 'Y'
                FROM library_schema.user_image user_image
                WHERE history.user_image_id = user_image.user_image_id
                  AND user_image.user_id = ?
                  AND user_image.image_id = ?
                  AND history.del_yn = 'N'
                """,
                userId,
                imageId
        );

        softDeleteDocumentRelations(userId, imageId, documentIds);
        softDeleteScheduleRelations(userId, imageId);
        recalculateCategories(userId, categoryIds);

        return ImageDeleteStatus.DELETED;
    }

    private boolean hasUserImage(Integer userId, Integer imageId, String delYn) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM library_schema.user_image
                WHERE user_id = ? AND image_id = ? AND del_yn = ?
                """,
                Integer.class,
                userId,
                imageId,
                delYn
        );
        return count != null && count > 0;
    }

    private List<Integer> findActiveDocumentIds(Integer userId, Integer imageId) {
        return jdbcTemplate.queryForList(
                """
                SELECT image_document.document_id
                FROM library_schema.image_document image_document
                JOIN library_schema.user_document user_document
                  ON user_document.document_id = image_document.document_id
                 AND user_document.user_id = ?
                 AND user_document.del_yn = 'N'
                WHERE image_document.image_id = ?
                  AND image_document.del_yn = 'N'
                """,
                Integer.class,
                userId,
                imageId
        );
    }

    private List<Integer> findActiveCategoryIds(Integer userId, Integer imageId) {
        return jdbcTemplate.queryForList(
                """
                SELECT DISTINCT category_id
                FROM query_schema.user_image_view
                WHERE user_id = ?
                  AND image_id = ?
                  AND del_yn = 'N'
                  AND category_id IS NOT NULL
                """,
                Integer.class,
                userId,
                imageId
        );
    }

    private void softDeleteDocumentRelations(
            Integer userId,
            Integer imageId,
            List<Integer> documentIds
    ) {
        for (Integer documentId : documentIds) {
            jdbcTemplate.update(
                    """
                    UPDATE library_schema.image_document
                    SET del_yn = 'Y', updated_at = now()
                    WHERE document_id = ? AND image_id = ? AND del_yn = 'N'
                    """,
                    documentId,
                    imageId
            );
            jdbcTemplate.update(
                    """
                    UPDATE query_schema.document_image_view
                    SET del_yn = 'Y', updated_at = now()
                    WHERE user_id = ?
                      AND document_id = ?
                      AND image_id = ?
                      AND del_yn = 'N'
                    """,
                    userId,
                    documentId,
                    imageId
            );
            jdbcTemplate.update(
                    """
                    UPDATE query_schema.user_document_view document_view
                    SET del_image_count = document_view.del_image_count + 1,
                        image_count = (
                            SELECT COUNT(*)
                            FROM library_schema.image_document image_document
                            WHERE image_document.document_id = ?
                              AND image_document.del_yn = 'N'
                        ),
                        updated_at = now()
                    WHERE document_view.user_id = ?
                      AND document_view.document_id = ?
                      AND document_view.del_yn = 'N'
                    """,
                    documentId,
                    userId,
                    documentId
            );
        }
    }

    private void softDeleteScheduleRelations(Integer userId, Integer imageId) {
        jdbcTemplate.update(
                """
                UPDATE library_schema.image_schedule image_schedule
                SET del_yn = 'Y'
                WHERE image_schedule.image_id = ?
                  AND image_schedule.del_yn = 'N'
                  AND EXISTS (
                      SELECT 1
                      FROM library_schema.user_schedule user_schedule
                      WHERE user_schedule.user_id = ?
                        AND user_schedule.schedule_id = image_schedule.schedule_id
                        AND user_schedule.del_yn = 'N'
                  )
                """,
                imageId,
                userId
        );
    }

    private void recalculateCategories(Integer userId, List<Integer> categoryIds) {
        for (Integer categoryId : categoryIds) {
            jdbcTemplate.update(
                    """
                    UPDATE query_schema.user_category_view category_view
                    SET image_count = (
                            SELECT COUNT(*)
                            FROM query_schema.user_image_view image_view
                            WHERE image_view.user_id = ?
                              AND image_view.category_id = ?
                              AND image_view.del_yn = 'N'
                              AND image_view.analysis_status IN ('COMPLETED', 'EMPTY')
                        ),
                        latest_uploaded_at = (
                            SELECT MAX(image_view.uploaded_at)
                            FROM query_schema.user_image_view image_view
                            WHERE image_view.user_id = ?
                              AND image_view.category_id = ?
                              AND image_view.del_yn = 'N'
                              AND image_view.analysis_status IN ('COMPLETED', 'EMPTY')
                        )
                    WHERE category_view.user_id = ?
                      AND category_view.category_id = ?
                      AND category_view.del_yn = 'N'
                    """,
                    userId,
                    categoryId,
                    userId,
                    categoryId,
                    userId,
                    categoryId
            );
        }
    }
}
