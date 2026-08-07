package com.ssafy.modera.api.domain.image.entity;

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

@Entity
@Table(name = "thumbnail", schema = "image_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Thumbnail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "thumbnail_id")
    private Integer thumbnailId;

    @Column(name = "image_id", nullable = false, unique = true)
    private Integer imageId;

    @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @Builder
    public Thumbnail(Integer imageId, String s3Key) {
        this.imageId = imageId;
        this.s3Key = s3Key;
    }
}
