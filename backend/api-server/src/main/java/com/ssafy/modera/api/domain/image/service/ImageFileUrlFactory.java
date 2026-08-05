package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.global.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

/**
 * 원본 이미지(pictures 버킷) s3_key -> presigned GET URL 변환.
 *
 * <p>썸네일용 {@link ThumbnailUrlFactory}와 버킷만 다르고 규칙은 같다. DB에는 key만
 * 저장하고 URL은 요청할 때마다 새로 만든다(CLAUDE.md: presigned URL을 DB에 저장하지
 * 않는다).
 *
 * <p>유효시간은 조회용 GET presign의 기존 값인 1시간을 그대로 쓴다. 업로드용 PUT은
 * 10분이지만 그건 용도가 다른 설정이라 끌어오지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ImageFileUrlFactory {

    /** 조회용 GET presign의 기존 유효시간(ThumbnailUrlFactory·ImageQueryService와 동일). */
    private static final Duration IMAGE_URL_TTL = Duration.ofHours(1);

    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    public String createViewUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket().getPictures())
                .key(s3Key)
                .build();

        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(IMAGE_URL_TTL)
                        .getObjectRequest(getObjectRequest)
                        .build())
                .url()
                .toString();
    }
}
