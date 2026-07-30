package com.ssafy.modera.worker.domain.document.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 문서 재료로 쓸 OCR(refined) 일괄 조회.
 *
 * ocr_refined_text는 analysis_result에만 있다 — worker가 문서 생성의 경유지가
 * 되는 이유가 이 컬럼이다. 같은 이미지가 여러 번 분석됐을 수 있으므로 이미지당
 * 최신 result 한 행만 고른다(SimilarImageRepository의 DISTINCT ON 패턴).
 */
@Repository
@RequiredArgsConstructor
public class DocumentOcrRepository {

    private final JdbcTemplate jdbcTemplate;

    /** 조회 결과에 없는 imageId는 맵에도 없다(분석 이력 없음 → OCR null로 취급). */
    public Map<Integer, String> findLatestRefinedOcr(List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT DISTINCT ON (image_id)
                       image_id,
                       ocr_refined_text
                FROM analysis_result
                WHERE image_id = ANY(?)
                ORDER BY image_id, result_id DESC
                """;

        Map<Integer, String> result = new HashMap<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setArray(1, con.createArrayOf("integer", imageIds.toArray(new Integer[0])));
            return ps;
        }, rs -> {
            result.put(rs.getInt("image_id"), rs.getString("ocr_refined_text"));
        });
        return result;
    }
}
