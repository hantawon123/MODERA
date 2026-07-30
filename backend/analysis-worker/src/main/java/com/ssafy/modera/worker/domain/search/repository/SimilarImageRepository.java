package com.ssafy.modera.worker.domain.search.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SimilarImageRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 기준 이미지와 임베딩이 가까운 순으로 같은 사용자의 이미지를 찾는다.
     *
     * - EMPTY 이미지는 embedding이 없으므로 자연히 제외
     * - 기준 이미지 자신은 제외
     * - 기준 이미지에 벡터가 없으면(비정보성) 서브쿼리가 NULL이 되어 결과가 0건이다.
     */
    public List<SimilarImageRow> findSimilar(int imageId, int userId, int limit) {
        String sql = """
                WITH base AS (
                    SELECT embedding
                    FROM analysis_result
                    WHERE image_id = ? AND embedding IS NOT NULL
                    ORDER BY result_id DESC
                    LIMIT 1
                ),
                candidates AS (
                    SELECT DISTINCT ON (r.image_id)
                           r.image_id,
                           r.embedding
                    FROM analysis_result r
                    JOIN analysis_job j ON j.job_id = r.job_id
                    WHERE j.user_id = ?
                      AND r.image_id <> ?
                      AND r.embedding IS NOT NULL
                    ORDER BY r.image_id, r.result_id DESC
                )
                SELECT c.image_id,
                       1 - (c.embedding <=> b.embedding) AS score
                FROM candidates c, base b
                ORDER BY c.embedding <=> b.embedding
                LIMIT ?
                """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new SimilarImageRow(
                        rs.getInt("image_id"),
                        rs.getFloat("score")),
                imageId, userId, imageId, limit);
    }

    public record SimilarImageRow(int imageId, float score) {}
}
