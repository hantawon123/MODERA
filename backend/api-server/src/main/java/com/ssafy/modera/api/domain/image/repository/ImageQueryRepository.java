package com.ssafy.modera.api.domain.image.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class ImageQueryRepository {

    private static final String UPSERT_SQL = """
            INSERT INTO query_schema.user_image_view (
                user_id, image_id, file_name, s3_key, thumbnail_key,
                title, summary, category_id, category_name, tags, key_information,
                structured_data, upload_status, analysis_status, favorite,
                uploaded_at, del_yn
            ) VALUES (
                ?, ?, ?, ?, ?, ?, ?,
                (SELECT category_id FROM taxonomy_schema.category WHERE name = ?),
                ?,
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
                ?, ?, ?, ?, ?, ?, 'N'
            )
            ON CONFLICT (user_id, image_id) DO UPDATE SET
                file_name = EXCLUDED.file_name,
                s3_key = EXCLUDED.s3_key,
                thumbnail_key = EXCLUDED.thumbnail_key,
                title = EXCLUDED.title,
                summary = EXCLUDED.summary,
                category_id = EXCLUDED.category_id,
                category_name = EXCLUDED.category_name,
                tags = EXCLUDED.tags,
                key_information = EXCLUDED.key_information,
                structured_data = EXCLUDED.structured_data,
                upload_status = EXCLUDED.upload_status,
                analysis_status = EXCLUDED.analysis_status,
                favorite = EXCLUDED.favorite,
                uploaded_at = EXCLUDED.uploaded_at
            WHERE query_schema.user_image_view.del_yn = 'N'
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ImageQueryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
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
            ps.setObject(i++, row.uploadedAt());
            return ps;
        });
    }

    public Optional<UserImageViewDetail> findDetail(Integer userId, Integer imageId) {
        return jdbcTemplate.query(
                        """
                        SELECT image_view.s3_key,
                               image_view.thumbnail_key,
                               image_view.upload_status,
                               image_view.analysis_status,
                               image_view.title,
                               image_view.favorite,
                               image_view.summary,
                               image_view.category_name,
                               image_view.tags,
                               image_view.key_information,
                               image_view.structured_data,
                               image_view.is_documented_yn,
                               image_view.is_calendared_yn
                        FROM query_schema.user_image_view image_view
                        JOIN library_schema.user_image user_image
                          ON user_image.user_id = image_view.user_id
                         AND user_image.image_id = image_view.image_id
                         AND user_image.del_yn = 'N'
                        WHERE image_view.user_id = ?
                          AND image_view.image_id = ?
                          AND image_view.del_yn = 'N'
                        """,
                        (rs, rowNum) -> new UserImageViewDetail(
                                    rs.getString("s3_key"),
                                    rs.getString("thumbnail_key"),
                                    rs.getString("upload_status"),
                                    rs.getString("analysis_status"),
                                    rs.getString("title"),
                                    rs.getObject("favorite", Boolean.class),
                                    rs.getString("summary"),
                                    rs.getString("category_name"),
                                    parseTagNames(rs.getString("tags")),
                                    toStringList(rs.getArray("key_information")),
                                    rs.getString("structured_data"),
                                    "Y".equals(rs.getString("is_documented_yn")),
                                    "Y".equals(rs.getString("is_calendared_yn"))
                            ),
                        userId,
                        imageId
                )
                .stream()
                .findFirst();
    }

    public boolean copyExistingView(Integer userId, Integer imageId) {
        jdbcTemplate.update(
                """
                DELETE FROM query_schema.user_image_view
                WHERE user_id = ? AND image_id = ? AND del_yn = 'Y'
                """,
                userId,
                imageId
        );

        return jdbcTemplate.update(
                """
                INSERT INTO query_schema.user_image_view (
                    user_id, image_id, file_name, s3_key, thumbnail_key,
                    title, summary, category_id, category_name, tags,
                    key_information, structured_data, upload_status, analysis_status,
                    favorite, uploaded_at, del_yn, is_documented_yn, is_calendared_yn
                )
                SELECT ?, image_id, file_name, s3_key, thumbnail_key,
                       title, summary, category_id, category_name, tags,
                       key_information, structured_data, upload_status, analysis_status,
                       false, uploaded_at, 'N', false, false
                FROM query_schema.user_image_view
                WHERE image_id = ?
                  AND del_yn = 'N'
                  AND analysis_status IN ('COMPLETED', 'EMPTY')
                ORDER BY CASE WHEN user_id = ? THEN 0 ELSE 1 END,
                         uploaded_at DESC NULLS LAST
                LIMIT 1
                ON CONFLICT (user_id, image_id) DO UPDATE SET
                    file_name = EXCLUDED.file_name,
                    s3_key = EXCLUDED.s3_key,
                    thumbnail_key = EXCLUDED.thumbnail_key,
                    title = EXCLUDED.title,
                    summary = EXCLUDED.summary,
                    category_id = EXCLUDED.category_id,
                    category_name = EXCLUDED.category_name,
                    tags = EXCLUDED.tags,
                    key_information = EXCLUDED.key_information,
                    structured_data = EXCLUDED.structured_data,
                    upload_status = EXCLUDED.upload_status,
                    analysis_status = EXCLUDED.analysis_status,
                    uploaded_at = EXCLUDED.uploaded_at
                WHERE query_schema.user_image_view.del_yn = 'N'
                """,
                userId,
                imageId,
                userId
        ) > 0;
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
                WHERE user_id = ? AND image_id = ? AND del_yn = 'N'
                """,
                analysisStatus,
                userId,
                imageId
        );
    }

    public void markUploadProcessing(Integer userId, Integer imageId) {
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_image_view
                SET upload_status = 'UPLOADED',
                    analysis_status = 'PROCESSING'
                WHERE user_id = ? AND image_id = ? AND del_yn = 'N'
                """,
                userId,
                imageId
        );
    }

    public ImageListPage findImages(
            Integer userId,
            Boolean favorite,
            Integer categoryId,
            String keyword,
            String sort,
            int page,
            int size
    ) {
        StringBuilder where = new StringBuilder("""
                FROM query_schema.user_image_view image_view
                JOIN library_schema.user_image user_image
                  ON user_image.user_id = image_view.user_id
                 AND user_image.image_id = image_view.image_id
                 AND user_image.del_yn = 'N'
                WHERE image_view.user_id = ?
                  AND image_view.del_yn = 'N'
                  AND image_view.analysis_status IN ('COMPLETED', 'EMPTY')
                """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);

        if (favorite != null) {
            where.append(" AND image_view.favorite = ?");
            parameters.add(favorite);
        }
        if (categoryId != null) {
            where.append(" AND image_view.category_id = ?");
            parameters.add(categoryId);
        }
        if (keyword != null) {
            String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
            where.append("""
                     AND (
                         LOWER(COALESCE(image_view.title, '')) LIKE ? ESCAPE '!'
                         OR EXISTS (
                             SELECT 1
                             FROM jsonb_array_elements(image_view.tags) tag
                             WHERE LOWER(COALESCE(tag ->> 'name', '')) LIKE ? ESCAPE '!'
                         )
                     )
                    """);
            parameters.add(pattern);
            parameters.add(pattern);
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) " + where,
                Long.class,
                parameters.toArray()
        );

        List<Object> contentParameters = new ArrayList<>(parameters);
        contentParameters.add(size);
        contentParameters.add(page * size);
        List<ImageListRow> content = jdbcTemplate.query(
                """
                SELECT image_view.image_id, image_view.title, image_view.summary,
                       image_view.favorite, image_view.thumbnail_key, image_view.tags,
                       image_view.category_name, image_view.uploaded_at
                """ + where + orderBy(sort) + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> new ImageListRow(
                        rs.getInt("image_id"),
                        rs.getString("title"),
                        rs.getString("summary"),
                        rs.getObject("favorite", Boolean.class),
                        rs.getString("thumbnail_key"),
                        parseTagNames(rs.getString("tags")),
                        rs.getString("category_name"),
                        rs.getObject("uploaded_at", java.time.OffsetDateTime.class)
                ),
                contentParameters.toArray()
        );

        return new ImageListPage(List.copyOf(content), total == null ? 0L : total);
    }

    private String orderBy(String sort) {
        return switch (sort) {
            case "TITLE_ASC" ->
                    " ORDER BY LOWER(COALESCE(image_view.title, '')) ASC, image_view.image_id ASC";
            case "UPLOADED_ASC" ->
                    " ORDER BY image_view.uploaded_at ASC NULLS LAST, image_view.image_id ASC";
            default ->
                    " ORDER BY image_view.uploaded_at DESC NULLS LAST, image_view.image_id ASC";
        };
    }

    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private List<String> parseTagNames(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode tags = objectMapper.readTree(tagsJson);
            List<String> names = new ArrayList<>();
            for (JsonNode tag : tags) {
                JsonNode name = tag.get("name");
                if (name != null && !name.isNull()) {
                    names.add(name.asText());
                }
            }
            return List.copyOf(names);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("이미지 태그 조회 모델 JSON을 읽을 수 없습니다.", exception);
        }
    }

    private String[] toArray(List<String> values) {
        return values == null ? new String[0] : values.toArray(new String[0]);
    }

    private List<String> toStringList(java.sql.Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return List.of();
        }
        Object value = sqlArray.getArray();
        if (!(value instanceof String[] strings)) {
            return List.of();
        }
        return List.of(strings);
    }

    private PGobject toJsonb(String json) throws SQLException {
        PGobject jsonb = new PGobject();
        jsonb.setType("jsonb");
        jsonb.setValue(json);
        return jsonb;
    }
    
    public List<UserImageViewSummary> findAllByUserIdAndImageIdIn(
            Integer userId,
            List<Integer> imageIds
    ) {
        if (imageIds == null || imageIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(
                ",",
                Collections.nCopies(imageIds.size(), "?")
        );

        String sql = """
                SELECT image_view.image_id,
                       image_view.title,
                       image_view.summary,
                       image_view.favorite,
                       image_view.thumbnail_key,
                       image_view.tags,
                       image_view.category_name
                FROM query_schema.user_image_view image_view
                JOIN library_schema.user_image user_image
                  ON user_image.user_id = image_view.user_id
                 AND user_image.image_id = image_view.image_id
                 AND user_image.del_yn = 'N'
                WHERE image_view.user_id = ?
                  AND image_view.del_yn = 'N'
                  AND image_view.image_id IN (%s)
                """.formatted(placeholders);

        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        parameters.addAll(imageIds);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new UserImageViewSummary(
                        rs.getInt("image_id"),
                        rs.getString("title"),
                        rs.getString("summary"),
                        rs.getObject("favorite", Boolean.class),
                        rs.getString("thumbnail_key"),
                        parseTagNames(rs.getString("tags")),
                        rs.getString("category_name")
                ),
                parameters.toArray()
        );
    }
}
