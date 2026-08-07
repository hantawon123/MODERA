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

import java.time.OffsetDateTime;

/**
 * 이미지와 문서의 논리 관계. {@code (document_id, image_id)} UNIQUE로 같은 이미지가
 * 한 문서에 두 번 붙지 않는다. 012에 있던 sort_order는 018에서 삭제되어 문서 안에서의
 * 이미지 순서는 저장하지 않는다.
 */
@Entity
@Table(name = "image_document", schema = "library_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_document_id")
    private Integer imageDocumentId;

    @Column(name = "image_id", nullable = false)
    private Integer imageId;

    @Column(name = "document_id", nullable = false)
    private Integer documentId;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "del_yn", nullable = false, length = 1)
    private String delYn = "N";

    @Builder
    public ImageDocument(Integer imageId, Integer documentId, OffsetDateTime updatedAt) {
        this.imageId = imageId;
        this.documentId = documentId;
        this.updatedAt = updatedAt;
    }
}
