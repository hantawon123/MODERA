package com.ssafy.modera.api.domain.category.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CategoryCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<CategoryReanalysisTarget> prepareRequest(
            Integer userId, Integer imageId, UUID requestId) {
        Integer userImageId = jdbcTemplate.query(
                """
                SELECT ui.user_image_id
                FROM library_schema.user_image ui
                JOIN query_schema.user_image_view view
                  ON view.user_id = ui.user_id
                 AND view.image_id = ui.image_id
                 AND view.del_yn = 'N'
                WHERE ui.user_id = ?
                  AND ui.image_id = ?
                  AND ui.del_yn = 'N'
                  AND ui.pending_category_request_id IS NULL
                  AND view.analysis_status IN ('COMPLETED', 'EMPTY')
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getInt(1),
                userId, imageId
        ).stream().findFirst().orElse(null);
        if (userImageId == null) {
            return Optional.empty();
        }

        List<Integer> excluded = jdbcTemplate.queryForList(
                """
                WITH candidates AS (
                    SELECT category_id, created_at,
                           user_image_category_history_id AS history_id
                    FROM library_schema.user_image_category_history
                    WHERE user_image_id = ?
                    UNION ALL
                    SELECT COALESCE(
                               ui.current_category_id,
                               view.category_id,
                               category.category_id
                           ),
                           'infinity'::timestamptz,
                           2147483647
                    FROM library_schema.user_image ui
                    JOIN query_schema.user_image_view view
                      ON view.user_id = ui.user_id
                     AND view.image_id = ui.image_id
                     AND view.del_yn = 'N'
                    LEFT JOIN taxonomy_schema.category category
                      ON category.name = view.category_name
                     AND category.del_yn = 'N'
                    WHERE ui.user_image_id = ?
                      AND COALESCE(
                              ui.current_category_id,
                              view.category_id,
                              category.category_id
                          ) IS NOT NULL
                ),
                deduplicated AS (
                    SELECT DISTINCT ON (category_id)
                           category_id, created_at, history_id
                    FROM candidates
                    ORDER BY category_id, created_at DESC, history_id DESC
                ),
                recent AS (
                    SELECT category_id, created_at, history_id
                    FROM deduplicated
                    ORDER BY created_at DESC, history_id DESC
                    LIMIT 5
                )
                SELECT category_id
                FROM recent
                ORDER BY created_at ASC, history_id ASC
                """,
                Integer.class,
                userImageId,
                userImageId
        );
        if (excluded.isEmpty()) {
            return Optional.empty();
        }
        int updated = jdbcTemplate.update(
                """
                UPDATE library_schema.user_image
                SET pending_category_request_id = ?
                WHERE user_image_id = ?
                  AND pending_category_request_id IS NULL
                """,
                requestId, userImageId
        );
        if (updated != 1) {
            return Optional.empty();
        }
        return Optional.of(new CategoryReanalysisTarget(
                userImageId, List.copyOf(excluded)));
    }

    public boolean applyResult(
            UUID requestId, Integer userId, Integer imageId,
            Integer categoryId, String categoryName) {
        Integer userImageId = jdbcTemplate.query(
                """
                SELECT user_image_id
                FROM library_schema.user_image
                WHERE user_id = ?
                  AND image_id = ?
                  AND del_yn = 'N'
                  AND pending_category_request_id = ?
                FOR UPDATE
                """,
                (rs, rowNum) -> rs.getInt(1),
                userId, imageId, requestId
        ).stream().findFirst().orElse(null);
        if (userImageId == null) {
            return false;
        }

        upsertTaxonomy(categoryId, categoryName);
        jdbcTemplate.update(
                """
                INSERT INTO library_schema.user_image_category_history (
                    user_image_id, category_id, source
                ) VALUES (?, ?, 'REANALYSIS')
                """,
                userImageId, categoryId
        );
        jdbcTemplate.update(
                """
                DELETE FROM library_schema.user_image_category_history
                WHERE user_image_id = ?
                  AND user_image_category_history_id NOT IN (
                      SELECT user_image_category_history_id
                      FROM library_schema.user_image_category_history
                      WHERE user_image_id = ?
                      ORDER BY created_at DESC,
                               user_image_category_history_id DESC
                      LIMIT 5
                  )
                """,
                userImageId, userImageId
        );
        jdbcTemplate.update(
                """
                UPDATE library_schema.user_image
                SET current_category_id = ?,
                    pending_category_request_id = NULL
                WHERE user_image_id = ?
                """,
                categoryId, userImageId
        );
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_image_view
                SET category_id = ?, category_name = ?
                WHERE user_id = ? AND image_id = ? AND del_yn = 'N'
                """,
                categoryId, categoryName.trim(), userId, imageId
        );
        return true;
    }

    public void clearPending(UUID requestId, Integer userId, Integer imageId) {
        jdbcTemplate.update(
                """
                UPDATE library_schema.user_image
                SET pending_category_request_id = NULL
                WHERE user_id = ? AND image_id = ?
                  AND pending_category_request_id = ? AND del_yn = 'N'
                """,
                userId, imageId, requestId
        );
    }

    public void initializeFromDefault(Integer userId, Integer imageId) {
        jdbcTemplate.update(
                """
                UPDATE library_schema.user_image ui
                SET current_category_id = default_category.category_id
                FROM library_schema.image_category default_category
                WHERE ui.user_id = ? AND ui.image_id = ?
                  AND ui.del_yn = 'N'
                  AND ui.current_category_id IS NULL
                  AND default_category.image_id = ui.image_id
                  AND default_category.del_yn = 'N'
                """,
                userId, imageId
        );
        jdbcTemplate.update(
                """
                INSERT INTO library_schema.user_image_category_history (
                    user_image_id, category_id, source
                )
                SELECT ui.user_image_id, ui.current_category_id, 'INITIAL'
                FROM library_schema.user_image ui
                WHERE ui.user_id = ? AND ui.image_id = ?
                  AND ui.del_yn = 'N'
                  AND ui.current_category_id IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM library_schema.user_image_category_history history
                      WHERE history.user_image_id = ui.user_image_id
                  )
                """,
                userId, imageId
        );
    }

    public void saveInitialDefault(
            Integer userId, Integer imageId,
            Integer categoryId, String categoryName) {
        if (categoryId == null || categoryName == null || categoryName.isBlank()) {
            return;
        }
        upsertTaxonomy(categoryId, categoryName);
        jdbcTemplate.update(
                """
                INSERT INTO library_schema.image_category (
                    image_id, category_id, del_yn
                ) VALUES (?, ?, 'N')
                ON CONFLICT (image_id) DO NOTHING
                """,
                imageId, categoryId
        );
        initializeFromDefault(userId, imageId);
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_image_view
                SET category_id = ?, category_name = ?
                WHERE user_id = ? AND image_id = ? AND del_yn = 'N'
                """,
                categoryId, categoryName.trim(), userId, imageId
        );
    }

    private void upsertTaxonomy(Integer categoryId, String categoryName) {
        jdbcTemplate.update(
                """
                INSERT INTO taxonomy_schema.category (
                    category_id, name, del_yn
                ) VALUES (?, ?, 'N')
                ON CONFLICT (category_id) DO UPDATE SET
                    name = EXCLUDED.name,
                    del_yn = 'N'
                """,
                categoryId, categoryName.trim()
        );
    }
}
