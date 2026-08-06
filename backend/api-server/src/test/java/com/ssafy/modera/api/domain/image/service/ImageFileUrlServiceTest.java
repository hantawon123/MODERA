package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.entity.Thumbnail;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.image.repository.ThumbnailRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * URL 발급(5-8/5-9)의 접근 통제·폴백 검증.
 *
 * <p>핵심 규약은 "접근할 수 없는 이미지는 이유를 구분하지 않고 전부 404"다. 403으로
 * 나누면 imageId를 순회해 존재 여부를 알아낼 수 있다.
 */
class ImageFileUrlServiceTest {

    private static final Integer USER_ID = 7;
    private static final Integer IMAGE_ID = 101;
    private static final String ORIGINAL_KEY = "7/101-a.jpg";
    private static final String THUMBNAIL_KEY = "7/101-thumb.jpg";
    private static final String ORIGINAL_URL = "http://localhost:9002/pictures/" + ORIGINAL_KEY + "?X-Amz-Signature=o";
    private static final String THUMBNAIL_URL = "http://localhost:9002/thumbnails/" + THUMBNAIL_KEY + "?X-Amz-Signature=t";

    private UserImageRepository userImageRepository;
    private ImageAssetRepository imageAssetRepository;
    private ThumbnailRepository thumbnailRepository;
    private ImageFileUrlFactory imageFileUrlFactory;
    private ThumbnailUrlFactory thumbnailUrlFactory;
    private ImageFileUrlService service;

    @BeforeEach
    void setUp() {
        userImageRepository = mock(UserImageRepository.class);
        imageAssetRepository = mock(ImageAssetRepository.class);
        thumbnailRepository = mock(ThumbnailRepository.class);
        imageFileUrlFactory = mock(ImageFileUrlFactory.class);
        thumbnailUrlFactory = mock(ThumbnailUrlFactory.class);
        service = new ImageFileUrlService(
                userImageRepository, imageAssetRepository, thumbnailRepository,
                imageFileUrlFactory, thumbnailUrlFactory);
    }

    @Test
    @DisplayName("본인 이미지는 원본 presigned URL을 돌려준다")
    void returnsPresignedOriginalForOwnedImage() {
        givenOwnedUploadedImage();
        when(imageFileUrlFactory.createViewUrl(ORIGINAL_KEY)).thenReturn(ORIGINAL_URL);

        assertThat(service.getOriginalUrl(USER_ID, IMAGE_ID)).isEqualTo(ORIGINAL_URL);
    }

    @Test
    @DisplayName("본인 이미지는 썸네일 presigned URL을 돌려준다")
    void returnsPresignedThumbnailForOwnedImage() {
        givenOwnedUploadedImage();
        Thumbnail thumbnail = thumbnail();
        when(thumbnailRepository.findByImageId(IMAGE_ID))
                .thenReturn(Optional.of(thumbnail));
        when(thumbnailUrlFactory.createViewUrl(THUMBNAIL_KEY)).thenReturn(THUMBNAIL_URL);

        assertThat(service.getThumbnailUrl(USER_ID, IMAGE_ID)).isEqualTo(THUMBNAIL_URL);
    }

    @Test
    @DisplayName("타인 소유 이미지는 404다(403으로 존재를 알리지 않는다)")
    void hidesUnownedImageBehind404() {
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(USER_ID, IMAGE_ID, "N"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOriginalUrl(USER_ID, IMAGE_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.IMAGE_NOT_FOUND);
        assertThatThrownBy(() -> service.getThumbnailUrl(USER_ID, IMAGE_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.IMAGE_NOT_FOUND);

        // 소유권이 없으면 키 조회·서명까지 가지 않는다.
        verify(imageAssetRepository, never()).findByImageIdAndDelYn(any(), anyString());
        verify(imageFileUrlFactory, never()).createViewUrl(anyString());
    }

    @Test
    @DisplayName("없거나 소프트 삭제된 이미지는 404다")
    void hidesMissingOrDeletedImageBehind404() {
        // 사용자 관계는 남아 있어도 자산이 삭제(del_yn='Y')됐으면 조회되지 않는다.
        UserImage userImage = mock(UserImage.class);
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(USER_ID, IMAGE_ID, "N"))
                .thenReturn(Optional.of(userImage));
        when(imageAssetRepository.findByImageIdAndDelYn(IMAGE_ID, "N"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOriginalUrl(USER_ID, IMAGE_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.IMAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("업로드가 끝나지 않은 이미지는 404다(S3에 객체가 없다)")
    void hidesPendingUploadBehind404() {
        UserImage userImage = mock(UserImage.class);
        ImageAsset pending = asset("PENDING");
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(USER_ID, IMAGE_ID, "N"))
                .thenReturn(Optional.of(userImage));
        when(imageAssetRepository.findByImageIdAndDelYn(IMAGE_ID, "N"))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.getOriginalUrl(USER_ID, IMAGE_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ImageErrorCode.IMAGE_NOT_FOUND);
        verify(imageFileUrlFactory, never()).createViewUrl(anyString());
    }

    @Test
    @DisplayName("썸네일이 아직 없으면 원본 키로 폴백한다")
    void fallsBackToOriginalWhenThumbnailMissing() {
        givenOwnedUploadedImage();
        when(thumbnailRepository.findByImageId(IMAGE_ID)).thenReturn(Optional.empty());
        when(imageFileUrlFactory.createViewUrl(ORIGINAL_KEY)).thenReturn(ORIGINAL_URL);

        assertThat(service.getThumbnailUrl(USER_ID, IMAGE_ID)).isEqualTo(ORIGINAL_URL);
        verify(thumbnailUrlFactory, never()).createViewUrl(anyString());
    }

    @Test
    @DisplayName("호출할 때마다 새로 서명한다(캐시하지 않는다)")
    void signsOnEveryCall() {
        givenOwnedUploadedImage();
        when(imageFileUrlFactory.createViewUrl(ORIGINAL_KEY))
                .thenReturn(ORIGINAL_URL + "1")
                .thenReturn(ORIGINAL_URL + "2");

        String first = service.getOriginalUrl(USER_ID, IMAGE_ID);
        String second = service.getOriginalUrl(USER_ID, IMAGE_ID);

        assertThat(first).isNotEqualTo(second);
        verify(imageFileUrlFactory, times(2)).createViewUrl(eq(ORIGINAL_KEY));
    }

    private void givenOwnedUploadedImage() {
        // 목 스터빙을 먼저 끝내고 나서 바깥 when()에 넘긴다 — 진행 중인 스터빙 안에서
        // 다시 when()을 부르면 Mockito가 UnfinishedStubbingException을 던진다.
        UserImage userImage = mock(UserImage.class);
        ImageAsset asset = asset("UPLOADED");
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(USER_ID, IMAGE_ID, "N"))
                .thenReturn(Optional.of(userImage));
        when(imageAssetRepository.findByImageIdAndDelYn(IMAGE_ID, "N"))
                .thenReturn(Optional.of(asset));
    }

    private ImageAsset asset(String uploadStatus) {
        ImageAsset asset = mock(ImageAsset.class);
        when(asset.getUploadStatus()).thenReturn(uploadStatus);
        when(asset.getS3Key()).thenReturn(ORIGINAL_KEY);
        return asset;
    }

    private Thumbnail thumbnail() {
        Thumbnail thumbnail = mock(Thumbnail.class);
        when(thumbnail.getS3Key()).thenReturn(THUMBNAIL_KEY);
        return thumbnail;
    }
}
