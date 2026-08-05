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
 * <p>호출처는 아이콘 리다이렉트(6-2, CategoryQueryService.getThumbnailRedirectUrl)
 * 하나다 — 목록(6-1)에는 presign 대신 불변 경로가 나간다(앱이 Room에 저장·캐시 키로
 * 쓰기 위해).
 *
 * <p>생성은 AI 쪽 백그라운드(수십 초)라, 카테고리가 막 생긴 직후에는 객체가 아직
 * 없어 URL이 404일 수 있다. 존재 확인(HEAD)을 넣지 않는다 — 앱이 이미지 404를
 * 플레이스홀더로 처리하고 다음 로드에서 자연 복구된다.
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
