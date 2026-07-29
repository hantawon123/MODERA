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
 * Logical relationship between a user object and an image object.
 * Cross-schema physical foreign keys are intentionally not used.
 */
@Entity
@Table(name = "user_image", schema = "library_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_image_id")
    private Integer userImageId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "image_id", nullable = false)
    private Integer imageId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "del_yn", nullable = false, length = 1)
    private String delYn = "N";

    @Builder
    public UserImage(Integer userId, Integer imageId) {
        this.userId = userId;
        this.imageId = imageId;
    }

    public void softDelete() {
        this.delYn = "Y";
    }

    public void restore() {
        this.delYn = "N";
    }
}
