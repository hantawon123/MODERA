package com.ssafy.modera.api.domain.document.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 문서 원본·관계의 상태 변경(재분석 시 관계 정리, 문서 삭제).
 *
 * <p>조회 모델 갱신은 {@link DocumentViewRepository}가, 새 관계 추가는 JPA 저장소가
 * 맡는다(image_document의 PK가 document_image_view의 PK로 그대로 쓰여서, 새 행은
 * 저장 후 생성된 ID가 필요하다). 여기 남은 건 "이미 있는 행을 끄는" 작업뿐이다.
 */
@Repository
@RequiredArgsConstructor
public class DocumentCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    /** 문서에 현재 살아 있는 이미지 관계. 재분석 때 무엇을 끄고 무엇을 새로 넣을지 판단한다. */
    public List<Integer> findActiveImageIds(Integer documentId) {
        return jdbcTemplate.queryForList(
                """
                SELECT image_id
                  FROM library_schema.image_document
                 WHERE document_id = ?
                   AND del_yn = 'N'
                 ORDER BY image_document_id
                """,
                Integer.class,
                documentId
        );
    }

    /**
     * 재분석 결과에 새로 들어온 이미지의 관계를 만든다. 이미 있으면 되살린다.
     *
     * <p>단순 INSERT를 쓸 수 없다 — UNIQUE(document_id, image_id)가 걸려 있어서, 앞선
     * 재분석에서 빠졌다가(del_yn='Y') 다시 들어온 이미지는 제약 위반으로 트랜잭션 전체를
     * 깬다. "한 번 빠진 이미지는 다시 넣을 수 없다"는 건 재분석 기능 자체와 모순이므로
     * 되살리는 쪽이 맞다.
     *
     * @return 관계 행의 PK. document_image_view의 PK로 그대로 쓴다.
     */
    public Integer upsertRelation(Integer documentId, Integer imageId, OffsetDateTime now) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO library_schema.image_document (document_id, image_id, updated_at, del_yn)
                VALUES (?, ?, ?, 'N')
                ON CONFLICT (document_id, image_id)
                DO UPDATE SET del_yn = 'N', updated_at = EXCLUDED.updated_at
                RETURNING image_document_id
                """, Integer.class, documentId, imageId, now);
    }

    /**
     * 관계 하나를 조회 모델에 복사한다. 되살린 관계면 그 행의 이미지 정보도 최신으로
     * 갱신한다 — 재분석 사이에 제목·요약이 바뀌었을 수 있다.
     *
     * @return 복사 여부. false면 그 이미지의 user_image_view 행이 없다(그 사이 삭제됨).
     */
    public boolean upsertDocumentImageView(Integer imageDocumentId, Integer userId,
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
                ON CONFLICT (image_document_id)
                DO UPDATE SET del_yn = 'N',
                              title = EXCLUDED.title,
                              summary = EXCLUDED.summary,
                              thumbnail_key = EXCLUDED.thumbnail_key,
                              tags = EXCLUDED.tags,
                              updated_at = EXCLUDED.updated_at
                """, imageDocumentId, userId, documentId, now, userId, imageId) > 0;
    }

    /**
     * 재분석 결과에서 빠진 이미지의 관계를 끈다.
     *
     * <p>del_image_count는 건드리지 않는다 — 재분석은 조회 모델 전체를 다시 쓰면서 그
     * 값을 0으로 되돌리기 때문이다(이미지 삭제 경로와 다른 점).
     */
    public void softDeleteRelations(Integer userId, Integer documentId,
                                    List<Integer> imageIds, OffsetDateTime now) {
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(imageIds.size(), "?"));

        Object[] relationParameters = new Object[imageIds.size() + 2];
        relationParameters[0] = now;
        relationParameters[1] = documentId;
        for (int i = 0; i < imageIds.size(); i++) {
            relationParameters[i + 2] = imageIds.get(i);
        }
        jdbcTemplate.update("""
                UPDATE library_schema.image_document
                   SET del_yn = 'Y', updated_at = ?
                 WHERE document_id = ?
                   AND image_id IN (%s)
                   AND del_yn = 'N'
                """.formatted(placeholders), relationParameters);

        Object[] viewParameters = new Object[imageIds.size() + 3];
        viewParameters[0] = now;
        viewParameters[1] = userId;
        viewParameters[2] = documentId;
        for (int i = 0; i < imageIds.size(); i++) {
            viewParameters[i + 3] = imageIds.get(i);
        }
        jdbcTemplate.update("""
                UPDATE query_schema.document_image_view
                   SET del_yn = 'Y', updated_at = ?
                 WHERE user_id = ?
                   AND document_id = ?
                   AND image_id IN (%s)
                   AND del_yn = 'N'
                """.formatted(placeholders), viewParameters);
    }

    /**
     * 문서 삭제(8-5). 원본·소유 관계·이미지 관계·조회 모델을 한꺼번에 끈다.
     *
     * <p>이미지 원본과 user_image 관계는 건드리지 않는다 — 문서를 지운다고 재료였던
     * 스크린샷까지 사라지면 안 된다. 문서화 표시(is_documented_yn) 복구는 호출자가
     * {@link DocumentViewRepository#unmarkDocumentedIfOrphan}로 처리한다(다른 문서에
     * 아직 포함돼 있을 수 있어 이 메서드만으로는 판단할 수 없다).
     */
    public void softDeleteDocument(Integer userId, Integer documentId, OffsetDateTime now) {
        jdbcTemplate.update("""
                UPDATE library_schema.image_document
                   SET del_yn = 'Y', updated_at = ?
                 WHERE document_id = ?
                   AND del_yn = 'N'
                """, now, documentId);
        jdbcTemplate.update("""
                UPDATE library_schema.user_document
                   SET del_yn = 'Y'
                 WHERE user_id = ?
                   AND document_id = ?
                   AND del_yn = 'N'
                """, userId, documentId);
        jdbcTemplate.update("""
                UPDATE query_schema.document_image_view
                   SET del_yn = 'Y', updated_at = ?
                 WHERE user_id = ?
                   AND document_id = ?
                   AND del_yn = 'N'
                """, now, userId, documentId);
        jdbcTemplate.update("""
                UPDATE query_schema.user_document_view
                   SET del_yn = 'Y', updated_at = ?
                 WHERE user_id = ?
                   AND document_id = ?
                   AND del_yn = 'N'
                """, now, userId, documentId);
        jdbcTemplate.update("""
                UPDATE document_schema.document
                   SET del_yn = 'Y', updated_at = ?
                 WHERE document_id = ?
                   AND del_yn = 'N'
                """, now, documentId);
    }
}
