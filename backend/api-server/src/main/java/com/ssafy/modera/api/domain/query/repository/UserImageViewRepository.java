package com.ssafy.modera.api.domain.query.repository;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
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

    /**
     * imageId 여러 건을 한 번에 조회한다. 반환 순서는 보장하지 않는다 —
     * 호출자가 원하는 순서(예: worker가 준 유사도 순)로 재정렬해서 쓴다.
     * <p>
     * 삭제된 이미지는 제외한다(5-1 목록의 조회 규칙과 동일하게 view의 del_yn만 본다.
     * library_schema.user_image.del_yn은 호출자가 소유권 검증할 때 함께 확인한다).
     */
    public List<UserImageViewSummary> findAllByUserIdAndImageIdIn(Integer userId, List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", Collections.nCopies(imageIds.size(), "?"));
        String sql = """
                SELECT image_id,
                       title,
                       summary,
                       favorite,
                       thumbnail_key,
                       category_name,
                       COALESCE(
                           (
                               SELECT array_agg(tag ->> 'name' ORDER BY tag_order)
                               FROM jsonb_array_elements(image_view.tags)
                                   WITH ORDINALITY AS source_tag(tag, tag_order)
                           ),
                           '{}'::TEXT[]
                       ) AS tag_names
                FROM query_schema.user_image_view AS image_view
                WHERE user_id = ?
                  AND del_yn = 'N'
                  AND image_id IN (%s)
                """.formatted(placeholders);

        Object[] parameters = new Object[imageIds.size() + 1];
        parameters[0] = userId;
        for (int i = 0; i < imageIds.size(); i++) {
            parameters[i + 1] = imageIds.get(i);
        }

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new UserImageViewSummary(
                        rs.getInt("image_id"),
                        rs.getString("title"),
                        rs.getString("summary"),
                        rs.getObject("favorite", Boolean.class),
                        rs.getString("thumbnail_key"),
                        toStringList(rs.getArray("tag_names")),
                        rs.getString("category_name")
                ),
                parameters
        );
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

    private List<String> toStringList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }
        return List.of((String[]) sqlArray.getArray());
    }

    private PGobject toJsonb(String json) throws SQLException {
        PGobject jsonb = new PGobject();
        jsonb.setType("jsonb");
        jsonb.setValue(json);
        return jsonb;
    }
}
