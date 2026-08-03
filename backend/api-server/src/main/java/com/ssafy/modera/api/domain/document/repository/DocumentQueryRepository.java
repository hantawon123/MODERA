package com.ssafy.modera.api.domain.document.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 문서 조회(8-1·8-3·8-4). 전부 query_schema의 조회 모델만 읽는다.
 *
 * <p>소유권 검증이 따로 없는 이유는 조회 자체가 검증이기 때문이다 — 두 조회 모델 모두
 * user_id를 PK/조건에 갖고 있어 남의 문서는 애초에 행이 돌아오지 않는다.
 */
@Repository
@RequiredArgsConstructor
public class DocumentQueryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public long countDocuments(Integer userId) {
        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM query_schema.user_document_view
                 WHERE user_id = ?
                   AND del_yn = 'N'
                """,
                Long.class,
                userId
        );
        return total == null ? 0L : total;
    }

    public List<DocumentListRow> findDocuments(Integer userId, String sort, int page, int size) {
        return jdbcTemplate.query(
                """
                SELECT document_id, name, summary, image_count, del_image_count, updated_at
                  FROM query_schema.user_document_view
                 WHERE user_id = ?
                   AND del_yn = 'N'
                """ + orderBy(sort) + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> new DocumentListRow(
                        rs.getInt("document_id"),
                        rs.getString("name"),
                        rs.getString("summary"),
                        rs.getInt("image_count"),
                        rs.getInt("del_image_count"),
                        rs.getObject("updated_at", OffsetDateTime.class)
                ),
                userId, size, page * size
        );
    }

    public Optional<DocumentDetailRow> findDocument(Integer userId, Integer documentId) {
        List<DocumentDetailRow> rows = jdbcTemplate.query(
                """
                SELECT document_id, name, summary, content, image_count, del_image_count, updated_at
                  FROM query_schema.user_document_view
                 WHERE user_id = ?
                   AND document_id = ?
                   AND del_yn = 'N'
                """,
                (rs, rowNum) -> new DocumentDetailRow(
                        rs.getInt("document_id"),
                        rs.getString("name"),
                        rs.getString("summary"),
                        rs.getString("content"),
                        rs.getInt("image_count"),
                        rs.getInt("del_image_count"),
                        rs.getObject("updated_at", OffsetDateTime.class)
                ),
                userId, documentId
        );
        return rows.stream().findFirst();
    }

    public boolean existsDocument(Integer userId, Integer documentId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM query_schema.user_document_view
                 WHERE user_id = ?
                   AND document_id = ?
                   AND del_yn = 'N'
                """,
                Integer.class,
                userId, documentId
        );
        return count != null && count > 0;
    }

    /** 재분석 기본 재료. 포함된 순서(=문서에 들어간 순서)를 유지한다. */
    public List<Integer> findDocumentImageIds(Integer userId, Integer documentId) {
        return jdbcTemplate.queryForList(
                """
                SELECT image_id
                  FROM query_schema.document_image_view
                 WHERE user_id = ?
                   AND document_id = ?
                   AND del_yn = 'N'
                 ORDER BY updated_at, image_document_id
                """,
                Integer.class,
                userId, documentId
        );
    }

    /**
     * 문서 구성 이미지 전체. 한 문서의 이미지는 최대 30장
     * (DocumentCommandService.MAX_IMAGES)이라 페이지를 나누지 않는다.
     *
     * <p>업로드 시각이 이 조회 모델에 없어 "문서에 포함된 시각"의 역순으로 준다.
     * 정렬 키가 같을 때 순서가 흔들리지 않도록 image_document_id를 tie-breaker로 붙인다.
     */
    public List<DocumentImageRow> findDocumentImages(Integer userId, Integer documentId) {
        return jdbcTemplate.query(
                """
                SELECT image_id, title, summary, thumbnail_key, tags, updated_at
                  FROM query_schema.document_image_view
                 WHERE user_id = ?
                   AND document_id = ?
                   AND del_yn = 'N'
                 ORDER BY updated_at DESC, image_document_id DESC
                """,
                (rs, rowNum) -> new DocumentImageRow(
                        rs.getInt("image_id"),
                        rs.getString("title"),
                        rs.getString("summary"),
                        rs.getString("thumbnail_key"),
                        parseTagNames(rs.getString("tags")),
                        rs.getObject("updated_at", OffsetDateTime.class)
                ),
                userId, documentId
        );
    }

    /** 정렬 키가 같을 때 순서가 흔들리지 않도록 document_id를 항상 tie-breaker로 붙인다. */
    private String orderBy(String sort) {
        return switch (sort) {
            case "UPDATED_ASC" -> " ORDER BY updated_at ASC, document_id ASC";
            case "NAME_ASC" -> " ORDER BY LOWER(name) ASC, document_id ASC";
            default -> " ORDER BY updated_at DESC, document_id DESC";
        };
    }

    /** tags는 {tagId, name} 객체 배열(JSONB)이고 응답에는 이름만 나간다(5-1과 동일). */
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
            throw new IllegalStateException("문서 이미지 태그 조회 모델 JSON을 읽을 수 없습니다.", exception);
        }
    }
}
