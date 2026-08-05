package com.ssafy.modera.api.domain.category.service;

import com.ssafy.modera.api.global.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

/**
 * 카테고리 아이콘 presigned GET URL. AI 서버가 카테고리 판정 시 아이콘을 생성해
 * category-thumbnails 버킷에 <b>{categoryId}.png</b>로 올려둔다(ai/ai_main/app/
 * category_icon.py). categoryId가 이름의 해시라 키가 결정적이고, 그래서 DB에
 * 키를 저장하거나 AI를 호출하지 않고 목록 조회 시점에 키를 조립해 서명만 한다.
 *
 * <p>생성은 AI 쪽 백그라운드(수십 초)라, 카테고리가 막 생긴 직후에는 객체가 아직
 * 없어 URL이 404일 수 있다. 존재 확인(HEAD)을 행마다 넣으면 목록 API가 MinIO
 * 왕복에 묶이므로 하지 않는다 — 앱이 이미지 404를 플레이스홀더로 처리한다.
 */
@Component
@RequiredArgsConstructor
public class CategoryImageUrlFactory {

    private static final Duration URL_TTL = Duration.ofHours(1);

    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    public String createViewUrl(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket().getCategoryThumbnails())
                .key(categoryId + ".png")
                .build();
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(URL_TTL)
                        .getObjectRequest(getObjectRequest)
                        .build())
                .url()
                .toString();
    }
}
