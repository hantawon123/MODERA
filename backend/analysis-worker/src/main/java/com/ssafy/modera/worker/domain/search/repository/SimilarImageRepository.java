package com.ssafy.modera.worker.domain.search.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SimilarImageRepository {

    /**
     * 단일 검색(연관 이미지)의 최소 코사인 유사도.
     *
     * <p>단일 검색은 개수 상한이 없어서 이 값이 결과 크기를 정하는 유일한 장치다 —
     * 관련이 애매한 것을 보여주는 것보다 비우는 편이 낫다.
     * 스키마 변경이 아니라 조회 시점 필터라 값 조정에 마이그레이션이 필요 없다.
     *
     * <p>0.65 → 0.78 (2026-08-09): 이 벡터(Gemini 요약 임베딩) 공간의 실측에서
     * "같은 카테고리" 수준의 매칭이 코사인 0.53~0.79(중앙값 0.73)에 분포한다 —
     * AI 서버의 카테고리 동일성 판정 임계도 0.62다. 즉 0.65는 "대충 같은
     * 카테고리"를 통과시키는 값이라, 내용이 무관한 이미지가 연관 이미지로 떴다.
     * "같은 주제"는 그 위쪽이므로 0.78로 올린다. 결과가 지나치게 마르면 이 값을
     * 내리기 전에 실제 쌍 분포(1 - (a.embedding <=> b.embedding))를 먼저 확인할 것.
     */
    private static final double SINGLE_MIN_SCORE = 0.78;

    /**
     * 다중 기준 검색의 최소 코사인 유사도. 후보가 <b>기준 이미지 중 한 장과의
     * 최대 유사도</b>로 이 값을 넘어야 살아남는다(컷). 순위는 centroid 코사인이다.
     *
     * <p>컷을 centroid로 하지 않는 이유: centroid는 기준이 늘수록 어느 개별
     * 문서와도 멀어져서, 같은 임계값이 기준 1장일 때와 4장일 때 전혀 다른
     * 세기로 걸린다 — k에 따라 캘리브레이션이 무효가 된다. 최대 유사도는 k와
     * 무관하므로 임계값을 한 번만 정하면 된다.
     *
     * <p>단일 검색(0.78)보다 낮게 두는 이유: 문서화 추천은 상위 N 컷(LIMIT)이
     * 이미 정크 노출을 제한하고 있고, "이것도 관련 있어요" 목록은 비우는 것보다
     * 후하게 보여주는 쪽이 낫다. 정크가 눈에 띄면 이 값을 단일과 같은 기준으로
     * 올린다(이제 의미가 단일 검색과 같은 쌍 단위 유사도라 그대로 비교 가능).
     */
    private static final double MULTI_MIN_SCORE = 0.65;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 기준 이미지와 임베딩이 가까운 순으로 같은 사용자의 이미지를 찾는다.
     *
     * - EMPTY 이미지는 embedding이 없으므로 자연히 제외
     * - 기준 이미지 자신은 제외
     * - 유사도(1 - 코사인 거리)가 SINGLE_MIN_SCORE 미만이면 제외
     * - 기준 이미지에 벡터가 없으면(비정보성) 서브쿼리가 NULL이 되어 결과가 0건이다.
     *
     * <p><b>개수 상한이 없다.</b> SINGLE_MIN_SCORE를 넘는 행을 전부 돌려준다. 유사한 이미지를
     * 많이 가진 사용자일수록 결과가 커지므로, 응답 크기가 문제가 되면 LIMIT이 아니라
     * SINGLE_MIN_SCORE를 올려서 조인다(관련도가 낮은 쪽부터 잘려야 한다).
     */
    public List<SimilarImageRow> findSimilar(int imageId, int userId) {
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
                """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new SimilarImageRow(
                        rs.getInt("image_id"),
                        rs.getFloat("score")),
                imageId, userId, imageId, SINGLE_MIN_SCORE);
    }

    /**
     * 여러 기준 이미지의 임베딩 평균(centroid)과 가까운 순으로 같은 사용자의 이미지를
     * 찾는다(5-7 문서화 관련 자료 검색).
     *
     * <p>평균을 쓰는 이유: 기준이 항공권·숙소 캡처라면 centroid는 "그 여행"쯤의 지점이
     * 되어, 개별 이미지 어느 하나가 아니라 공통 주제에 가까운 것이 잡힌다. 이미지별로
     * 검색해 합치는 방식보다 쿼리가 하나로 끝나고 순위 병합 규칙도 필요 없다.
     *
     * <p><b>순위와 컷을 분리한다.</b> 순위는 centroid 코사인(공통 주제 가까운 순),
     * 컷은 기준 중 한 장과의 최대 코사인 ≥ MULTI_MIN_SCORE(임계값 의미가 기준 수와
     * 무관 — 상수 주석 참고). 기준이 1장이면 centroid = 그 벡터라 두 기준이 같아진다.
     *
     * - 기준 이미지들 자신은 결과에서 제외
     * - 기준 이미지 전부에 임베딩이 없으면 AVG가 NULL이 되어 0건이다(비정보성만 고른 경우)
     */
    public List<SimilarImageRow> findSimilarToAll(List<Integer> imageIds, int userId, int limit) {
        String sql = """
                WITH latest AS (
                    SELECT DISTINCT ON (image_id) image_id, embedding
                    FROM analysis_result
                    WHERE image_id = ANY(?) AND embedding IS NOT NULL
                    ORDER BY image_id, result_id DESC
                ),
                base AS (
                    SELECT AVG(embedding) AS embedding FROM latest
                ),
                candidates AS (
                    SELECT DISTINCT ON (r.image_id)
                           r.image_id,
                           r.embedding
                    FROM analysis_result r
                    JOIN analysis_job j ON j.job_id = r.job_id
                    WHERE j.user_id = ?
                      AND r.image_id <> ALL(?)
                      AND r.embedding IS NOT NULL
                    ORDER BY r.image_id, r.result_id DESC
                )
                SELECT c.image_id,
                       1 - (c.embedding <=> b.embedding) AS score
                FROM candidates c, base b
                WHERE b.embedding IS NOT NULL
                  AND EXISTS (
                      SELECT 1 FROM latest l
                      WHERE 1 - (c.embedding <=> l.embedding) >= ?
                  )
                ORDER BY c.embedding <=> b.embedding
                LIMIT ?
                """;
        List<SimilarImageRow> rows = new ArrayList<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            Array ids = con.createArrayOf("integer", imageIds.toArray(new Integer[0]));
            ps.setArray(1, ids);
            ps.setInt(2, userId);
            ps.setArray(3, ids);
            ps.setDouble(4, MULTI_MIN_SCORE);
            ps.setInt(5, limit);
            return ps;
        }, rs -> {
            rows.add(new SimilarImageRow(rs.getInt("image_id"), rs.getFloat("score")));
        });
        return rows;
    }

    public record SimilarImageRow(int imageId, float score) {}
}
