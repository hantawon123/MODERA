package com.ssafy.modera.api.domain.query.repository;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

/**
 * embedding(vector(768))은 ANALYSIS_COMPLETED 이벤트 계약에 포함되어 있지 않아
 * 이 저장소는 건드리지 않는다(NULL로 남는다). 재인덱싱 배치 등 후속 작업의 몫.
 */
@Repository
public class ImageSearchDocumentRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO query_schema.image_search_document (
                image_id, user_id, title, summary, ocr_text, category_name, tag_names,
                structured_fields, indexed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (image_id) DO UPDATE SET
                user_id = EXCLUDED.user_id,
                title = EXCLUDED.title,
                summary = EXCLUDED.summary,
                ocr_text = EXCLUDED.ocr_text,
                category_name = EXCLUDED.category_name,
                tag_names = EXCLUDED.tag_names,
                structured_fields = EXCLUDED.structured_fields,
                indexed_at = EXCLUDED.indexed_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public ImageSearchDocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(ImageSearchDocumentRow row) {
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(UPSERT_SQL);
            int i = 1;
            ps.setInt(i++, row.imageId());
            ps.setInt(i++, row.userId());
            ps.setString(i++, row.title());
            ps.setString(i++, row.summary());
            ps.setString(i++, row.ocrText());
            ps.setString(i++, row.categoryName());
            ps.setArray(i++, con.createArrayOf("text", toArray(row.tagNames())));
            if (row.structuredFieldsJson() == null) {
                ps.setNull(i++, Types.OTHER);
            } else {
                ps.setObject(i++, toJsonb(row.structuredFieldsJson()));
            }
            ps.setTimestamp(i++, row.indexedAt() == null ? null : Timestamp.from(row.indexedAt()));
            return ps;
        });
    }

    private String[] toArray(List<String> values) {
        return values == null ? new String[0] : values.toArray(new String[0]);
    }

    private PGobject toJsonb(String json) throws SQLException {
        PGobject jsonb = new PGobject();
        jsonb.setType("jsonb");
        jsonb.setValue(json);
        return jsonb;
    }
}
