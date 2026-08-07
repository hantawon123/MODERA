package com.ssafy.modera.worker.domain.document.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 문서 재료로 쓸 OCR 일괄 조회.
 *
 * OCR은 analysis_result에만 있다 — worker가 문서 생성의 경유지가 되는 이유가 이
 * 테이블이다. 같은 이미지가 여러 번 분석됐을 수 있으므로 이미지당 최신 result 한 행만
 * 고른다(SimilarImageRepository의 DISTINCT ON 패턴).
 *
 * <p><b>refined만 읽으면 안 된다</b>: AI가 2026-07-29 합의로 콜백에서 ocrRefinedText를
 * 빼면서 ocr_refined_text는 항상 NULL이다. 실제 텍스트는 앱이 보낸 온디바이스 OCR이
 * 복사된 ocr_raw_text에 들어 있다(AnalysisCallbackService가 job의 client_ocr_raw_text를
 * 그대로 싣는다). 두 컬럼을 다 읽어 AI 쪽 우선순위(refined → raw)를 그대로 넘긴다.
 */
@Repository
@RequiredArgsConstructor
public class DocumentOcrRepository {

    private final JdbcTemplate jdbcTemplate;

    /** 조회 결과에 없는 imageId는 맵에도 없다(분석 이력 없음 → OCR 없음으로 취급). */
    public Map<Integer, DocumentOcr> findLatestOcr(List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return Map.of();
        }

        String sql = """
                SELECT DISTINCT ON (image_id)
                       image_id,
                       ocr_raw_text,
                       ocr_refined_text
                FROM analysis_result
                WHERE image_id = ANY(?)
                ORDER BY image_id, result_id DESC
                """;

        Map<Integer, DocumentOcr> result = new HashMap<>();
        jdbcTemplate.query(con -> {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setArray(1, con.createArrayOf("integer", imageIds.toArray(new Integer[0])));
            return ps;
        }, rs -> {
            result.put(rs.getInt("image_id"), new DocumentOcr(
                    rs.getString("ocr_raw_text"),
                    rs.getString("ocr_refined_text")
            ));
        });
        return result;
    }
}
