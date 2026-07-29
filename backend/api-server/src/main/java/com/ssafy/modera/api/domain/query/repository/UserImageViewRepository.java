package com.ssafy.modera.api.domain.query.repository;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

@Repository
public class UserImageViewRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO query_schema.user_image_view (
                user_id, image_id, file_name, s3_key, thumbnail_key,
                title, summary, category_name, tags, key_information,
                structured_data, upload_status, analysis_status, favorite
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?,
                (
                    SELECT COALESCE(
                        jsonb_agg(
                            jsonb_build_object(
                                'tagId', NULL,
                                'name', tag_name
                            )
                            ORDER BY tag_order
                        ),
                        '[]'::jsonb
                    )
                    FROM unnest(CAST(? AS text[]))
                        WITH ORDINALITY AS source_tag(tag_name, tag_order)
                ),
                ?, ?, ?, ?, ?
            )
            ON CONFLICT (user_id, image_id) DO UPDATE SET
                file_name = EXCLUDED.file_name,
                s3_key = EXCLUDED.s3_key,
                thumbnail_key = EXCLUDED.thumbnail_key,
                title = EXCLUDED.title,
                summary = EXCLUDED.summary,
                category_name = EXCLUDED.category_name,
                tags = EXCLUDED.tags,
                key_information = EXCLUDED.key_information,
                structured_data = EXCLUDED.structured_data,
                upload_status = EXCLUDED.upload_status,
                analysis_status = EXCLUDED.analysis_status,
                favorite = EXCLUDED.favorite
            """;

    private final JdbcTemplate jdbcTemplate;

    public UserImageViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(UserImageViewRow row) {
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(UPSERT_SQL);
            int i = 1;
            ps.setInt(i++, row.userId());
            ps.setInt(i++, row.imageId());
            ps.setString(i++, row.fileName());
            ps.setString(i++, row.s3Key());
            ps.setString(i++, row.thumbnailKey());
            ps.setString(i++, row.title());
            ps.setString(i++, row.summary());
            ps.setString(i++, row.categoryName());
            ps.setArray(i++, con.createArrayOf("text", toArray(row.tagNames())));
            ps.setArray(i++, con.createArrayOf("text", toArray(row.keyInformation())));
            if (row.structuredDataJson() == null) {
                ps.setNull(i++, Types.OTHER);
            } else {
                ps.setObject(i++, toJsonb(row.structuredDataJson()));
            }
            ps.setString(i++, row.uploadStatus());
            ps.setString(i++, row.analysisStatus());
            if (row.favorite() == null) {
                ps.setNull(i++, Types.BOOLEAN);
            } else {
                ps.setBoolean(i++, row.favorite());
            }
            return ps;
        });
    }

    public Optional<UserImageViewDetail> findDetail(Integer userId, Integer imageId) {
        return jdbcTemplate.query(
                        """
                        SELECT analysis_status, title, favorite
                        FROM query_schema.user_image_view
                        WHERE user_id = ? AND image_id = ?
                        """,
                        (rs, rowNum) -> {
                            return new UserImageViewDetail(
                                    rs.getString("analysis_status"),
                                    rs.getString("title"),
                                    rs.getObject("favorite", Boolean.class)
                            );
                        },
                        userId,
                        imageId
                )
                .stream()
                .findFirst();
    }

    public void updateAnalysisStatus(
            Integer userId,
            Integer imageId,
            String analysisStatus
    ) {
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_image_view
                SET analysis_status = ?
                WHERE user_id = ? AND image_id = ?
                """,
                analysisStatus,
                userId,
                imageId
        );
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
