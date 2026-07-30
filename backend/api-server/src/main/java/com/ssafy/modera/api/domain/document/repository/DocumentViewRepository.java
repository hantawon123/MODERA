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
    public void insertUserDocumentView(Integer userId, Integer documentId, String name, String summary,
                                       String content, int imageCount, OffsetDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO query_schema.user_document_view
                    (user_id, document_id, name, summary, content, image_count, del_image_count, updated_at, del_yn)
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, 'N')
                """, userId, documentId, name, summary, content, imageCount, now);
    }

    /**
     * 재분석 결과로 조회 모델을 갈아끼운다.
     *
     * <p>del_image_count를 0으로 되돌리는 게 핵심이다 — 이 값은 "문서를 만든 뒤 원본
     * 이미지가 삭제됐으니 갱신이 필요하다"는 표시인데, 방금 지금 있는 이미지들로 다시
     * 만들었으므로 갱신 필요 상태가 해소된 것이다.
     */
    public void updateUserDocumentView(Integer userId, Integer documentId, String name, String summary,
                                       String content, int imageCount, OffsetDateTime now) {
        jdbcTemplate.update("""
                UPDATE query_schema.user_document_view
                   SET name = ?,
                       summary = ?,
                       content = ?,
                       image_count = ?,
                       del_image_count = 0,
                       updated_at = ?
                 WHERE user_id = ?
                   AND document_id = ?
                   AND del_yn = 'N'
                """, name, summary, content, imageCount, now, userId, documentId);
    }

    /**
     * 문서에서 빠진 이미지의 문서화 표시를 끈다. 다른 활성 문서에 아직 포함되어 있으면
     * 그대로 둔다 — 이 값은 "어떤 문서에든 쓰였는가"를 뜻하기 때문이다.
     */
    public void unmarkDocumentedIfOrphan(Integer userId, List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(imageIds.size(), "?"));
        String sql = """
                UPDATE query_schema.user_image_view image_view
                   SET is_documented_yn = 'N'
                 WHERE image_view.user_id = ?
                   AND image_view.image_id IN (%s)
                   AND NOT EXISTS (
                       SELECT 1
                         FROM library_schema.image_document image_document
                         JOIN library_schema.user_document user_document
                           ON user_document.document_id = image_document.document_id
                          AND user_document.user_id = image_view.user_id
                          AND user_document.del_yn = 'N'
                        WHERE image_document.image_id = image_view.image_id
                          AND image_document.del_yn = 'N'
                   )
                """.formatted(placeholders);

        Object[] parameters = new Object[imageIds.size() + 1];
        parameters[0] = userId;
        for (int i = 0; i < imageIds.size(); i++) {
            parameters[i + 1] = imageIds.get(i);
        }
        jdbcTemplate.update(sql, parameters);
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
