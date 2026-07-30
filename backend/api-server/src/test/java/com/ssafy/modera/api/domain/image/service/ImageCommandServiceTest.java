package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.ImageRegistrationRequestRepository;
import com.ssafy.modera.api.domain.image.repository.OcrRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.global.config.StorageProperties;
import com.ssafy.modera.api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageCommandServiceTest {

    @Mock ImageAssetRepository imageAssetRepository;
    @Mock OcrRepository ocrRepository;
    @Mock ImageRegistrationRequestRepository registrationRequestRepository;
    @Mock UserImageRepository userImageRepository;
    @Mock ImageQueryRepository imageQueryRepository;
    @Mock StorageProperties storageProperties;
    @Mock S3Client s3Client;
    @Mock S3Presigner s3Presigner;
    @Mock PlatformTransactionManager transactionManager;
    @InjectMocks ImageCommandService imageCommandService;

    @Test
    void hidesMissingOrDeletedOwnershipAsNotFound() {
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(1, 10, "N"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> imageCommandService.reissueUploadUrl(1, 10))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ImageErrorCode.IMAGE_NOT_FOUND));
    }

    @Test
    void rejectsReissueAfterUploadCompleted() {
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(1, 10, "N"))
                .thenReturn(Optional.of(UserImage.builder().userId(1).imageId(10).build()));
        when(imageAssetRepository.findByImageIdAndDelYn(10, "N"))
                .thenReturn(Optional.of(ImageAsset.builder()
                        .imageId(10)
                        .fileName("image.jpg")
                        .contentHash("a".repeat(64))
                        .fileSize(100)
                        .s3Key("1/10-image.jpg")
                        .uploadStatus("UPLOADED")
                        .build()));

        assertThatThrownBy(() -> imageCommandService.reissueUploadUrl(1, 10))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ImageErrorCode.UPLOAD_ALREADY_COMPLETED));
    }
}
