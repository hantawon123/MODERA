package com.ssafy.modera.api.domain.category.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CategoryQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<CategoryListRow> findCategories(Integer userId, String orderBy) {
        return jdbcTemplate.query(
                """
                SELECT category_id,
                       category_name,
                       image_s3_key,
                       image_count,
                       latest_uploaded_at
                FROM query_schema.user_category_view
                WHERE user_id = ?
                  AND del_yn = 'N'
                ORDER BY %s
                """.formatted(orderBy),
                (rs, rowNum) -> new CategoryListRow(
                        rs.getObject("category_id", Integer.class),
                        rs.getString("category_name"),
                        rs.getString("image_s3_key"),
                        rs.getObject("image_count", Integer.class),
                        rs.getObject("latest_uploaded_at", java.time.OffsetDateTime.class)
                ),
                userId
        );
    }
}
