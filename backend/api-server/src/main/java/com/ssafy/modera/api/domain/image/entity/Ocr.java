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
@Table(name = "ocr", schema = "image_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ocr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ocr_id")
    private Integer ocrId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_id", nullable = false, unique = true)
    private Integer imageId;

    @Builder
    public Ocr(String content, Integer imageId) {
        this.content = content;
        this.imageId = imageId;
    }
}
