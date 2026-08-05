package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.image.repository.ThumbnailRepository;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 원본·썸네일 이미지의 302 리다이렉트용 presigned URL 발급.
 *
 * <p>앱이 저장하는 주소는 <b>{@code /api/v1/images/{imageId}/file}</b>·
 * <b>{@code .../thumbnail}</b>이라는 불변 경로이고, 그 순간의 presigned URL은 매 요청
 * 새로 만들어 Location으로만 내보낸다. 앱은 만료되는 URL을 저장하지 않아도 되고,
 * 이미지 캐시 키도 이 불변 경로로 안정된다.
 *
 * <p><b>미소유·미존재를 구분하지 않고 전부 404로 응답한다.</b> 403으로 나누면 imageId를
 * 순회하며 "있지만 남의 것"을 식별할 수 있어 리소스 열거가 가능해진다 — 이 프로젝트가
 * 유사 이미지·문서화·카테고리 썸네일에서 이미 쓰는 정책과 같다.
 *
 * <p>스키마 경계를 넘는 JOIN을 만들지 않기 위해(CLAUDE.md) 소유권은
 * {@code library_schema.user_image}, 원본 키는 {@code image_schema.image_asset},
 * 썸네일 키는 {@code image_schema.thumbnail}에서 각각 조회해 애플리케이션에서 조합한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageFileRedirectService {

    private static final String ACTIVE = "N";
    private static final String UPLOAD_STATUS_UPLOADED = "UPLOADED";

    private final UserImageRepository userImageRepository;
    private final ImageAssetRepository imageAssetRepository;
    private final ThumbnailRepository thumbnailRepository;
    private final ImageFileUrlFactory imageFileUrlFactory;
    private final ThumbnailUrlFactory thumbnailUrlFactory;

    /** 원본 이미지의 presigned GET URL. */
    @Transactional(readOnly = true)
    public String getOriginalRedirectUrl(Integer userId, Integer imageId) {
        ImageAsset imageAsset = readOwnedUploadedAsset(userId, imageId);
        return imageFileUrlFactory.createViewUrl(imageAsset.getS3Key());
    }

    /**
     * 썸네일의 presigned GET URL. 썸네일이 아직 없으면(분석 전 등) 원본 키로 폴백한다 —
     * 404보다 화면이 자연스럽고 클라이언트에서 분기할 필요가 없다.
     */
    @Transactional(readOnly = true)
    public String getThumbnailRedirectUrl(Integer userId, Integer imageId) {
        ImageAsset imageAsset = readOwnedUploadedAsset(userId, imageId);

        return thumbnailRepository.findByImageId(imageId)
                .map(thumbnail -> thumbnailUrlFactory.createViewUrl(thumbnail.getS3Key()))
                .orElseGet(() -> {
                    log.debug("썸네일이 아직 없어 원본으로 폴백한다: imageId={}", imageId);
                    return imageFileUrlFactory.createViewUrl(imageAsset.getS3Key());
                });
    }

    /**
     * 본인 소유의 업로드 완료 이미지만 돌려준다. 미소유·미존재·삭제·업로드 미완료는
     * 모두 IMAGE_NOT_FOUND(404)로 통일한다.
     */
    private ImageAsset readOwnedUploadedAsset(Integer userId, Integer imageId) {
        userImageRepository.findByUserIdAndImageIdAndDelYn(userId, imageId, ACTIVE)
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        ImageAsset imageAsset = imageAssetRepository.findByImageIdAndDelYn(imageId, ACTIVE)
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        // 업로드가 끝나기 전에는 S3에 객체가 없다. 서명은 되지만 열면 404라
        // 여기서 끊어 클라이언트가 "아직 없음"을 한 가지 코드로 다루게 한다.
        if (!UPLOAD_STATUS_UPLOADED.equals(imageAsset.getUploadStatus())) {
            throw new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND);
        }
        return imageAsset;
    }
}
