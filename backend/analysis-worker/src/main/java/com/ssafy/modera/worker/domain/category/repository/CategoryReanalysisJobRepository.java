package com.ssafy.modera.worker.domain.category.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CategoryReanalysisJobRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean create(
            UUID requestId, Integer userId, Integer imageId,
            List<Integer> excludedCategoryIds) {
        return jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(
                    """
                    INSERT INTO category_reanalysis_job (
                        category_request_id, user_id, image_id,
                        excluded_category_ids, status
                    ) VALUES (?, ?, ?, ?, 'PENDING')
                    ON CONFLICT (category_request_id) DO NOTHING
                    """);
            ps.setObject(1, requestId);
            ps.setInt(2, userId);
            ps.setInt(3, imageId);
            ps.setArray(4, con.createArrayOf(
                    "integer", excludedCategoryIds.toArray()));
            return ps;
        }) > 0;
    }

    public void updateStatus(UUID requestId, String status) {
        jdbcTemplate.update(
                """
                UPDATE category_reanalysis_job
                SET status = ?
                WHERE category_request_id = ?
                """,
                status, requestId
        );
    }
}
