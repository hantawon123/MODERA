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
import java.time.OffsetDateTime;
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
        synchronizeUserCategories(row.userId());
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
                               ocr.content AS ocr_raw_text,
                               image_view.uploaded_at,
                               image_view.is_documented_yn,
                               image_view.is_calendared_yn
                        FROM query_schema.user_image_view image_view
                        JOIN library_schema.user_image user_image
                          ON user_image.user_id = image_view.user_id
                         AND user_image.image_id = image_view.image_id
                         AND user_image.del_yn = 'N'
                        LEFT JOIN image_schema.ocr ocr
                          ON ocr.image_id = image_view.image_id
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
                                    rs.getString("ocr_raw_text"),
                                    rs.getObject("uploaded_at", java.time.OffsetDateTime.class),
                                    "Y".equals(rs.getString("is_documented_yn")),
                                    "Y".equals(rs.getString("is_calendared_yn"))
                            ),
                        userId,
                        imageId
                )
                .stream()
                .findFirst();
    }

    public boolean isAnalysisActiveOrCompleted(Integer userId, Integer imageId) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM query_schema.user_image_view
                    WHERE user_id = ?
                      AND image_id = ?
                      AND del_yn = 'N'
                      AND analysis_status IN ('PROCESSING', 'COMPLETED', 'EMPTY')
                )
                """,
                Boolean.class,
                userId,
                imageId
        );
        return Boolean.TRUE.equals(exists);
    }

    public boolean copyExistingView(Integer userId, Integer imageId) {
        boolean copied = jdbcTemplate.update(
                """
                INSERT INTO query_schema.user_image_view (
                    user_id, image_id, file_name, s3_key, thumbnail_key,
                    title, summary, category_id, category_name, tags,
                    key_information, structured_data, upload_status, analysis_status,
                    favorite, uploaded_at, del_yn, is_documented_yn, is_calendared_yn
                )
                SELECT ?, source.image_id, source.file_name, source.s3_key,
                       source.thumbnail_key, source.title, source.summary,
                       COALESCE(default_category.category_id, source.category_id),
                       COALESCE(category.name, source.category_name),
                       source.tags, source.key_information, source.structured_data,
                       source.upload_status, source.analysis_status,
                       false, source.uploaded_at, 'N', 'N', 'N'
                FROM query_schema.user_image_view source
                LEFT JOIN library_schema.image_category default_category
                  ON default_category.image_id = source.image_id
                 AND default_category.del_yn = 'N'
                LEFT JOIN taxonomy_schema.category category
                  ON category.category_id = default_category.category_id
                 AND category.del_yn = 'N'
                WHERE source.image_id = ?
                  AND source.analysis_status IN ('COMPLETED', 'EMPTY')
                ORDER BY CASE WHEN source.user_id = ? THEN 0 ELSE 1 END,
                         CASE WHEN source.del_yn = 'N' THEN 0 ELSE 1 END,
                         source.uploaded_at DESC NULLS LAST
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
                    favorite = false,
                    uploaded_at = EXCLUDED.uploaded_at,
                    del_yn = 'N',
                    is_documented_yn = 'N',
                    is_calendared_yn = 'N'
                """,
                userId,
                imageId,
                userId
        ) > 0;
        if (copied) {
            synchronizeUserCategories(userId);
        }
        return copied;
    }

    /**
     * 사용자 이미지 Read Model을 기준으로 카테고리 Read Model을 동기화한다.
     * 새 카테고리는 생성하고, 재분석으로 카테고리가 바뀐 경우 이전 카테고리는
     * 삭제하지 않은 채 image_count=0, latest_uploaded_at=null 상태로 유지한다.
     */
    public void synchronizeUserCategories(Integer userId) {
        jdbcTemplate.update(
                """
                INSERT INTO query_schema.user_category_view (
                    user_id,
                    category_id,
                    category_name,
                    image_count,
                    latest_uploaded_at,
                    image_s3_key,
                    del_yn
                )
                SELECT image_view.user_id,
                       image_view.category_id,
                       MAX(image_view.category_name),
                       COUNT(*)::INTEGER,
                       MAX(image_view.uploaded_at),
                       MAX(category.image_s3_key),
                       'N'
                FROM query_schema.user_image_view image_view
                JOIN library_schema.user_image user_image
                  ON user_image.user_id = image_view.user_id
                 AND user_image.image_id = image_view.image_id
                 AND user_image.del_yn = 'N'
                LEFT JOIN taxonomy_schema.category category
                  ON category.category_id = image_view.category_id
                 AND category.del_yn = 'N'
                WHERE image_view.user_id = ?
                  AND image_view.category_id IS NOT NULL
                  AND image_view.del_yn = 'N'
                GROUP BY image_view.user_id, image_view.category_id
                ON CONFLICT (user_id, category_id) DO UPDATE SET
                    category_name = EXCLUDED.category_name,
                    image_count = EXCLUDED.image_count,
                    latest_uploaded_at = EXCLUDED.latest_uploaded_at,
                    image_s3_key = EXCLUDED.image_s3_key,
                    del_yn = 'N'
                """,
                userId
        );

        jdbcTemplate.update(
                """
                UPDATE query_schema.user_category_view category_view
                SET image_count = (
                        SELECT COUNT(*)
                        FROM query_schema.user_image_view image_view
                        JOIN library_schema.user_image user_image
                          ON user_image.user_id = image_view.user_id
                         AND user_image.image_id = image_view.image_id
                         AND user_image.del_yn = 'N'
                        WHERE image_view.user_id = category_view.user_id
                          AND image_view.category_id = category_view.category_id
                          AND image_view.del_yn = 'N'
                    ),
                    latest_uploaded_at = (
                        SELECT MAX(image_view.uploaded_at)
                        FROM query_schema.user_image_view image_view
                        JOIN library_schema.user_image user_image
                          ON user_image.user_id = image_view.user_id
                         AND user_image.image_id = image_view.image_id
                         AND user_image.del_yn = 'N'
                        WHERE image_view.user_id = category_view.user_id
                          AND image_view.category_id = category_view.category_id
                          AND image_view.del_yn = 'N'
                    ),
                    image_s3_key = (
                        SELECT category.image_s3_key
                        FROM taxonomy_schema.category category
                        WHERE category.category_id = category_view.category_id
                          AND category.del_yn = 'N'
                    )
                WHERE category_view.user_id = ?
                  AND category_view.del_yn = 'N'
                """,
                userId
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
                WHERE user_id = ? AND image_id = ? AND del_yn = 'N'
                """,
                analysisStatus,
                userId,
                imageId
        );
    }

    public void updateCalendared(Integer userId, Integer imageId, boolean calendared) {
        jdbcTemplate.update(
                """
                UPDATE query_schema.user_image_view
                SET is_calendared_yn = ?
                WHERE user_id = ? AND image_id = ? AND del_yn = 'N'
                """,
                calendared ? "Y" : "N",
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
                       image_view.category_name, image_view.uploaded_at,
                       image_view.is_documented_yn, image_view.is_calendared_yn
                """ + where + orderBy(sort) + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> new ImageListRow(
                        rs.getInt("image_id"),
                        rs.getString("title"),
                        rs.getString("summary"),
                        rs.getObject("favorite", Boolean.class),
                        rs.getString("thumbnail_key"),
                        parseTagNames(rs.getString("tags")),
                        rs.getString("category_name"),
                        rs.getObject("uploaded_at", java.time.OffsetDateTime.class),
                        "Y".equals(rs.getString("is_documented_yn")),
                        "Y".equals(rs.getString("is_calendared_yn"))
                ),
                contentParameters.toArray()
        );

        return new ImageListPage(List.copyOf(content), total == null ? 0L : total);
    }

    private String orderBy(String sort) {
        return switch (sort) {
            case "TITLE_ASC" ->
                    " ORDER BY LOWER(COALESCE(image_view.title, '')) " +
                            "COLLATE \"ko-KR-x-icu\" ASC, image_view.image_id ASC";
            case "UPLOADED_ASC" ->
                    " ORDER BY image_view.uploaded_at ASC NULLS LAST, image_view.image_id ASC";
            default ->
                    " ORDER BY image_view.uploaded_at DESC NULLS LAST, image_view.image_id ASC";
        };
    }

    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /**
     * 문서 생성(8-2) 재료 조회. 소유권 검증과 재료 수집을 한 번에 한다 — user_image 조인이
     * 곧 "이 사용자 것인가"의 답이라, 돌아온 행 수가 요청한 imageIds보다 적으면 남의
     * 이미지이거나 삭제된 것이다.
     *
     * <p>순서는 보장하지 않는다(IN 절). 이벤트 payload의 이미지 순서는 클라이언트가 준
     * 순서를 따라야 하므로 서비스 계층에서 다시 정렬한다.
     */
    public List<DocumentSourceImage> findDocumentSources(Integer userId, List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", Collections.nCopies(imageIds.size(), "?"));
        String sql = """
                SELECT image_view.image_id,
                       image_view.title,
                       image_view.summary,
                       image_view.category_name,
                       image_view.tags,
                       image_view.key_information,
                       image_view.uploaded_at,
                       image_view.analysis_status
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
                (rs, rowNum) -> new DocumentSourceImage(
                        rs.getInt("image_id"),
                        rs.getString("title"),
                        rs.getString("summary"),
                        rs.getString("category_name"),
                        parseTagNames(rs.getString("tags")),
                        toStringList(rs.getArray("key_information")),
                        rs.getObject("uploaded_at", OffsetDateTime.class),
                        rs.getString("analysis_status")
                ),
                parameters.toArray()
        );
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

    public List<ImageListRow> findVisibleImagesByIds(
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
                       image_view.category_name,
                       image_view.uploaded_at,
                       image_view.is_documented_yn,
                       image_view.is_calendared_yn
                FROM query_schema.user_image_view image_view
                JOIN library_schema.user_image user_image
                  ON user_image.user_id = image_view.user_id
                 AND user_image.image_id = image_view.image_id
                 AND user_image.del_yn = 'N'
                WHERE image_view.user_id = ?
                  AND image_view.del_yn = 'N'
                  AND image_view.analysis_status IN ('COMPLETED', 'EMPTY')
                  AND image_view.image_id IN (%s)
                """.formatted(placeholders);

        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);
        parameters.addAll(imageIds);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ImageListRow(
                        rs.getInt("image_id"),
                        rs.getString("title"),
                        rs.getString("summary"),
                        rs.getObject("favorite", Boolean.class),
                        rs.getString("thumbnail_key"),
                        parseTagNames(rs.getString("tags")),
                        rs.getString("category_name"),
                        rs.getObject("uploaded_at", java.time.OffsetDateTime.class),
                        "Y".equals(rs.getString("is_documented_yn")),
                        "Y".equals(rs.getString("is_calendared_yn"))
                ),
                parameters.toArray()
        );
    }
}
