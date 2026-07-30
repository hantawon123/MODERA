package com.ssafy.modera.api.domain.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 사용자와 문서의 논리 관계. document_id에 단독 UNIQUE가 걸려 있어 문서 소유자는
 * 정확히 한 명이다(018). schema 경계를 넘는 물리 FK는 쓰지 않는다.
 */
@Entity
@Table(name = "user_document", schema = "library_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_document_id")
    private Integer userDocumentId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "document_id", nullable = false)
    private Integer documentId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "del_yn", nullable = false, length = 1)
    private String delYn = "N";

    @Builder
    public UserDocument(Integer userId, Integer documentId) {
        this.userId = userId;
        this.documentId = documentId;
    }
}
