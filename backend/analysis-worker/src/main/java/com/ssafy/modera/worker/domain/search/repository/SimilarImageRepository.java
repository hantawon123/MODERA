package com.ssafy.modera.worker.domain.search.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SimilarImageRepository {

    /**
     * 결과에 포함할 최소 코사인 유사도.
     *
     * <p>임계값이 없으면 LIMIT을 채우기 위해 관련 없는 이미지까지 순위만 매겨 내려간다 —
     * 사용자의 이미지가 몇 장 없을수록 "연관 자료"에 전혀 상관없는 것이 뜬다. 관련이
     * 애매한 것을 보여주는 것보다 비우는 편이 낫다는 판단으로 0.6을 기준으로 한다.
     * 스키마 변경이 아니라 조회 시점 필터라 값 조정에 마이그레이션이 필요 없다.
     */
    private static final double MIN_SCORE = 0.6;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 기준 이미지와 임베딩이 가까운 순으로 같은 사용자의 이미지를 찾는다.
     *
     * - EMPTY 이미지는 embedding이 없으므로 자연히 제외
     * - 기준 이미지 자신은 제외
     * - 유사도(1 - 코사인 거리)가 MIN_SCORE 미만이면 제외
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
                WHERE 1 - (c.embedding <=> b.embedding) >= ?
                ORDER BY c.embedding <=> b.embedding
                LIMIT ?
                """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new SimilarImageRow(
                        rs.getInt("image_id"),
                        rs.getFloat("score")),
                imageId, userId, imageId, MIN_SCORE, limit);
    }

    public record SimilarImageRow(int imageId, float score) {}
}
