package com.ssafy.modera.api.domain.image.repository;

public record UserImageViewDetail(
        String s3Key,
        String uploadStatus,
        String analysisStatus,
        String title,
        Boolean favorite
) {
}
