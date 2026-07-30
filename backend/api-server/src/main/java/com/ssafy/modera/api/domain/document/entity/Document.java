package com.ssafy.modera.api.domain.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 문서 원본(마크다운). 소유자와 포함 이미지는 library_schema가 관계로 들고 있어
 * 이 엔티티에는 없다 — schema 경계를 넘는 연관관계를 만들지 않는다.
 */
@Entity
@Table(name = "document", schema = "document_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

    /**
     * document.name 컬럼 폭. AI 제목에는 길이 상한이 없어서 이보다 길면 저장 전에 자른다.
     *
     * <p>컬럼을 넓히는 대신 자르는 쪽을 택했다 — 실제 제목은 이 길이 근처에도 가지 않고,
     * 넓혀도 상한이 사라지는 게 아니라 자르는 로직은 어차피 필요하기 때문이다. 조회 모델
     * (user_document_view.name)은 VARCHAR(255)로 더 넓지만 여기서 자른 값이 그대로
     * 복사되므로 실제로 어긋나지 않는다.
     */
    public static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Integer documentId;

    @Column(name = "name", nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "del_yn", nullable = false, length = 1)
    private String delYn = "N";

    public Document(String name, String content, OffsetDateTime now) {
        this.name = truncateName(name);
        this.content = content == null ? "" : content;
        this.updatedAt = now;
    }

    /**
     * 제목이 길다는 이유로 문서 전체를 버리는 것보다 잘라 저장하는 편이 낫다.
     * 자르지 않으면 INSERT가 실패해 트랜잭션 전체가 롤백되고 문서가 통째로 날아간다.
     */
    private static String truncateName(String name) {
        if (name == null || name.isBlank()) {
            return "제목 없는 문서";
        }
        return name.length() <= MAX_NAME_LENGTH ? name : name.substring(0, MAX_NAME_LENGTH);
    }
}
