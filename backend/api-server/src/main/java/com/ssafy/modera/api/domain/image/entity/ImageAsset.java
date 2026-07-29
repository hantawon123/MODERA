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

@Entity
@Table(name = "image_asset", schema = "image_schema")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageAsset {

    @Id
    @Column(name = "image_id")
    private Integer imageId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "file_size", nullable = false)
    private Integer fileSize;

    @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @Column(name = "upload_status", nullable = false)
    private String uploadStatus;

    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;

    @Builder
    public ImageAsset(Integer imageId, String fileName, String contentHash, Integer fileSize,
                       String s3Key, String uploadStatus) {
        this.imageId = imageId;
        this.fileName = fileName;
        this.contentHash = contentHash;
        this.fileSize = fileSize;
        this.s3Key = s3Key;
        this.uploadStatus = uploadStatus;
    }

    public void markUploaded(OffsetDateTime uploadedAt) {
        this.uploadStatus = "UPLOADED";
        this.uploadedAt = uploadedAt;
    }
}
