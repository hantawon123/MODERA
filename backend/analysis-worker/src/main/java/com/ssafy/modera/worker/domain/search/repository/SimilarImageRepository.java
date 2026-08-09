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
     * <p>0.72 (2026-08-09, 새 임베딩 공간 기준): AI 가 documentVector 재료를
     * 요약 단독에서 <b>제목+태그+요약 합성</b>으로 바꾸면서(주제 응집도 개선)
     * 분포가 통째로 이동했다 — 이 값은 새 공간에서 쌍 2,367개 분포 실측 +
     * 경계 구간 요약문 대조로 다시 잡은 것이다.
     *   무관 baseline: p50=0.588, p90=0.684 (구공간보다 +0.06 위)
     *   시리즈성 연관: 0.73~0.84 에 뭉침 — 채용공고끼리 0.736~0.760, 클로드
     *   가이드 시리즈 0.734~0.754(구공간 0.647), 현대차 취준 0.791~0.826,
     *   SSAFY 0.837, 걸음수 0.830~0.913
     *   경성 무관: 대부분 0.71 아래 (엑셀↔노래방 리모컨 0.703, 야구티켓↔축제
     *   0.710). 예외적으로 사연 공모↔자소서류가 0.738~0.745 까지 올라온다.
     * 0.72 에서 무관 혼입 ~3-4% (구공간 0.68 의 ~13% 대비 대폭 개선).
     * <b>주의: 이 값은 재임베딩 백필(scripts/backfill_document_vectors.py,
     * AI 레포)이 적용된 공간 전제다.</b> 백필 전 구공간에 이 값을 쓰면 다시
     * strict 해진다. 조정할 일이 생기면 감으로 만지지 말고 같은 방법(쌍 분포 +
     * 경계 구간 요약문 대조)으로 재실측할 것.
     *
     * <p>0.65 (2026-08-09 재완화, 팀 결정): 시연에서 후보가 마르는 것보다 정크
     * 혼입이 낫다는 판단 — 리콜 우선. 위 실측 기준 무관 baseline p90(0.684)보다
     * 낮아 무관 혼입이 늘어나는 것을 알고 내린 값이다.
     */
    private static final double SINGLE_MIN_SCORE = 0.65;

    /**
     * 다중 기준 검색의 최소 코사인 유사도. 후보가 <b>기준 이미지 중 한 장과의
     * 최대 유사도</b>로 이 값을 넘어야 살아남는다(컷). 순위는 centroid 코사인이다.
     *
     * <p>컷을 centroid로 하지 않는 이유: centroid는 기준이 늘수록 어느 개별
     * 문서와도 멀어져서, 같은 임계값이 기준 1장일 때와 4장일 때 전혀 다른
     * 세기로 걸린다 — k에 따라 캘리브레이션이 무효가 된다. 최대 유사도는 k와
     * 무관하므로 임계값을 한 번만 정하면 된다.
     *
     * <p>0.68 (2026-08-09, 새 임베딩 공간 기준 — SINGLE_MIN_SCORE 주석 참고):
     * 분포가 +0.06 이동해 구공간 0.65 와 같은 상대 위치다(전체 쌍의 ~10% 통과).
     * 단일보다 낮게 두는 이유 — 고르는 화면이라 후보가 마르는 비용이 정크
     * 몇 개보다 크고(무관 후보는 안 고르면 끝), 상위 N 컷(LIMIT)이 노출을 이미
     * 제한하며, centroid 순위가 진짜 연관을 앞에 세워 혼재 구간은 꼬리에만 남는다.
     *
     * <p>0.65 (2026-08-09 재완화, 팀 결정): SINGLE_MIN_SCORE 와 함께 리콜 우선으로
     * 하향 — 단일과 같은 값이 됐다.
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
