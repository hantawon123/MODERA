package com.ssafy.modera.api.domain.image.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "image_asset", schema = "image_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageAsset {

    @Id
    @Column(name = "image_id")
    private UUID imageId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @Column(name = "thumbnail_key")
    private String thumbnailKey;

    @Column(name = "upload_status", nullable = false)
    private String uploadStatus;

    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    public ImageAsset(UUID imageId, String fileName, String contentHash, Integer fileSize,
                       String sourceUrl, String s3Key, String thumbnailKey, String uploadStatus,
                       OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.imageId = imageId;
        this.fileName = fileName;
        this.contentHash = contentHash;
        this.fileSize = fileSize;
        this.sourceUrl = sourceUrl;
        this.s3Key = s3Key;
        this.thumbnailKey = thumbnailKey;
        this.uploadStatus = uploadStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void markUploaded(OffsetDateTime uploadedAt) {
        this.uploadStatus = "UPLOADED";
        this.uploadedAt = uploadedAt;
        this.updatedAt = uploadedAt;
    }
}
