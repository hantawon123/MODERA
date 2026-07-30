package com.ssafy.modera.api.domain.document.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * query_schema의 문서 조회 모델 갱신.
 *
 * <p>JPA 대신 JdbcTemplate을 쓰는 이유는 document_image_view.tags가 JSONB이기 때문이다
 * (CLAUDE.md 규칙). 다만 그 값을 자바로 끌어와 다시 넣지 않고 user_image_view에서
 * INSERT ... SELECT로 옮긴다 — 같은 schema 안이라 조회가 자유롭고, 태그 JSON을
 * 직렬화·역직렬화하는 왕복이 통째로 사라진다.
 */
@Repository
public class DocumentViewRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentViewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** created_at은 032에서 삭제되어 시간 기준은 updated_at 하나뿐이다. */
    public void insertUserDocumentView(Integer userId, Integer documentId, String name,
                                       String content, int imageCount, OffsetDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO query_schema.user_document_view
                    (user_id, document_id, name, content, image_count, del_image_count, updated_at, del_yn)
                VALUES (?, ?, ?, ?, ?, 0, ?, 'N')
                """, userId, documentId, name, content, imageCount, now);
    }

    /**
     * 문서에 포함된 이미지 한 건을 조회 모델에 복사한다.
     *
     * <p>document_image_view의 title·summary·thumbnail_key는 NOT NULL인데 user_image_view
     * 쪽은 분석 전이면 비어 있을 수 있어 COALESCE로 채운다. 행이 없으면(=사용자 소유가
     * 아니거나 삭제됨) 아무것도 넣지 않는다 — 접수 단계에서 이미 검증했으므로 정상 경로에서는
     * 발생하지 않지만, 그 사이 삭제된 경우 조용히 빠지는 편이 트랜잭션을 깨는 것보다 낫다.
     */
    public boolean insertDocumentImageView(Integer imageDocumentId, Integer userId,
                                           Integer documentId, Integer imageId, OffsetDateTime now) {
        return jdbcTemplate.update("""
                INSERT INTO query_schema.document_image_view
                    (image_document_id, user_id, document_id, image_id,
                     title, summary, thumbnail_key, tags, updated_at, del_yn)
                SELECT ?, ?, ?, image_view.image_id,
                       COALESCE(image_view.title, ''),
                       COALESCE(image_view.summary, ''),
                       COALESCE(image_view.thumbnail_key, ''),
                       image_view.tags,
                       ?, 'N'
                  FROM query_schema.user_image_view image_view
                 WHERE image_view.user_id = ?
                   AND image_view.image_id = ?
                   AND image_view.del_yn = 'N'
                """, imageDocumentId, userId, documentId, now, userId, imageId) > 0;
    }

    /** 문서에 포함된 이미지들의 문서화 여부를 켠다. 이미 'Y'면 그대로다. */
    public void markDocumented(Integer userId, List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(imageIds.size(), "?"));
        String sql = """
                UPDATE query_schema.user_image_view
                   SET is_documented_yn = 'Y'
                 WHERE user_id = ?
                   AND image_id IN (%s)
                """.formatted(placeholders);

        Object[] parameters = new Object[imageIds.size() + 1];
        parameters[0] = userId;
        for (int i = 0; i < imageIds.size(); i++) {
            parameters[i + 1] = imageIds.get(i);
        }
        jdbcTemplate.update(sql, parameters);
    }
}
