package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.global.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

/**
 * thumbnail_key -> presigned GET URL 변환. DB에는 key만 저장하고 URL은 조회할 때마다
 * 새로 만든다(CLAUDE.md: presigned URL을 DB에 저장하지 않는다).
 * <p>
 * 목록 성격의 조회(5-1 목록, 연관 자료)가 공통으로 쓰는 변환이라 별도 컴포넌트로 뺐다.
 * 서명은 S3Presigner가 public endpoint로 한다(S3Config 주석 참고).
 */
@Component
@RequiredArgsConstructor
public class ThumbnailUrlFactory {

    /** API 명세서 5-1: thumbnailUrl의 유효시간은 1시간 */
    private static final Duration THUMBNAIL_URL_TTL = Duration.ofHours(1);

    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    /** 썸네일이 아직 생성되지 않았으면(key가 null) null을 반환한다. */
    public String createViewUrl(String thumbnailKey) {
        if (thumbnailKey == null || thumbnailKey.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket().getThumbnails())
                .key(thumbnailKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(THUMBNAIL_URL_TTL)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
