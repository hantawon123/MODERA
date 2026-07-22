package com.ssafy.modera.worker.domain.analysis.repository;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * JPA 대신 JdbcTemplate을 쓰는 이유: embedding(vector(768))과 structured_fields/
 * key_information(jsonb)은 Hibernate 표준 매핑이 없어 직접 캐스팅/PGobject로 바인딩해야 한다.
 */
@Repository
public class AnalysisResultRepository {

    private static final String INSERT_SQL = """
            INSERT INTO analysis_result (
                job_id, image_id, ocr_raw_text, ocr_refined_text, ocr_lang, ocr_confidence,
                summary, informative, structured_type, structured_fields, key_information,
                analysis_confidence, embedding, model_version, analyzed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?)
            ON CONFLICT (image_id, model_version) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public AnalysisResultRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** @return true면 새로 저장됨, false면 이미 있던 (imageId, modelVersion) 조합이라 건너뜀 */
    public boolean insert(AnalysisResultRow row) {
        int updated = jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(INSERT_SQL);
            int i = 1;
            ps.setLong(i++, row.jobId());
            ps.setObject(i++, row.imageId());
            ps.setString(i++, row.ocrRawText());
            ps.setString(i++, row.ocrRefinedText());
            ps.setString(i++, row.ocrLang());
            setNullableFloat(ps, i++, row.ocrConfidence());
            ps.setString(i++, row.summary());
            setNullableBoolean(ps, i++, row.informative());
            ps.setString(i++, row.structuredType());
            setJsonbOrNull(ps, i++, row.structuredFieldsJson());
            setJsonbOrNull(ps, i++, row.keyInformationJson());
            setNullableFloat(ps, i++, row.analysisConfidence());
            ps.setString(i++, toVectorLiteral(row.embedding()));
            ps.setString(i++, row.modelVersion());
            ps.setTimestamp(i++, row.analyzedAt() == null ? null : Timestamp.from(row.analyzedAt()));
            return ps;
        });
        return updated > 0;
    }

    private void setNullableFloat(PreparedStatement ps, int index, Float value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.REAL);
        } else {
            ps.setFloat(index, value);
        }
    }

    private void setNullableBoolean(PreparedStatement ps, int index, Boolean value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BOOLEAN);
        } else {
            ps.setBoolean(index, value);
        }
    }

    private void setJsonbOrNull(PreparedStatement ps, int index, String json) throws SQLException {
        if (json == null) {
            ps.setNull(index, Types.OTHER);
            return;
        }
        PGobject jsonb = new PGobject();
        jsonb.setType("jsonb");
        jsonb.setValue(json);
        ps.setObject(index, jsonb);
    }

    private String toVectorLiteral(float[] embedding) {
        if (embedding == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(embedding.length * 8);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
