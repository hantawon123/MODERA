package com.ssafy.modera.api.domain.category.service;

import com.ssafy.modera.api.global.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CategoryImageUrlFactory {

    private static final Duration URL_TTL = Duration.ofHours(1);

    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    public String createViewUrl(String imageS3Key) {
        if (imageS3Key == null || imageS3Key.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket().getPictures())
                .key(imageS3Key)
                .build();
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(URL_TTL)
                        .getObjectRequest(getObjectRequest)
                        .build())
                .url()
                .toString();
    }
}
