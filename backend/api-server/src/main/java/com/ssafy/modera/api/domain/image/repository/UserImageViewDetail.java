package com.ssafy.modera.api.domain.image.repository;

import java.util.List;

public record UserImageViewDetail(
        String s3Key,
        String thumbnailKey,
        String uploadStatus,
        String analysisStatus,
        String title,
        Boolean favorite,
        String summary,
        String categoryName,
        List<String> tagNames,
        List<String> keyInformation,
        String structuredDataJson,
        Boolean documented,
        Boolean calendared
) {
}
