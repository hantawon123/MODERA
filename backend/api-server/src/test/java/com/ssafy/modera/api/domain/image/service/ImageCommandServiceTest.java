package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.dto.request.ImageRegisterItemRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageRegisterOcrRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageRegisterRequest;
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
import org.springframework.transaction.TransactionStatus;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock TransactionStatus transactionStatus;
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

    @Test
    void processesMultipleImagesIndependently() throws Exception {
        String newHash = "a".repeat(64);
        String duplicateHash = "b".repeat(64);
        UUID newRequestId = UUID.randomUUID();
        UUID duplicateRequestId = UUID.randomUUID();
        UUID invalidRequestId = UUID.randomUUID();

        ImageAsset duplicateAsset = ImageAsset.builder()
                .imageId(20)
                .fileName("existing.jpg")
                .contentHash(duplicateHash)
                .fileSize(200)
                .s3Key("1/20-existing.jpg")
                .uploadStatus("UPLOADED")
                .build();
        StorageProperties.Bucket bucket = new StorageProperties.Bucket();
        bucket.setPictures("pictures");

        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(storageProperties.getBucket()).thenReturn(bucket);
        when(registrationRequestRepository.findByUserIdAndClientRequestIdAndDelYn(
                any(), any(), any())).thenReturn(Optional.empty());
        when(registrationRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageAssetRepository.findByContentHashAndDelYn(newHash, "N")).thenReturn(Optional.empty());
        when(imageAssetRepository.findByContentHashAndDelYn(duplicateHash, "N"))
                .thenReturn(Optional.of(duplicateAsset));
        when(imageAssetRepository.nextImageId()).thenReturn(21);
        when(imageAssetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(1, 20, "N"))
                .thenReturn(Optional.of(UserImage.builder().userId(1).imageId(20).build()));
        when(imageQueryRepository.copyExistingView(1, 20)).thenReturn(true);
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        PresignedPutObjectRequest presignedRequest = org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create("https://storage.example/new-image").toURL());
        when(s3Presigner.presignPutObject(any(
                software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        ImageRegisterRequest request = new ImageRegisterRequest(List.of(
                new ImageRegisterItemRequest(
                        newRequestId,
                        "new.jpg",
                        newHash,
                        100,
                        new ImageRegisterOcrRequest("new image text")
                ),
                new ImageRegisterItemRequest(
                        duplicateRequestId,
                        "existing.jpg",
                        duplicateHash,
                        200,
                        new ImageRegisterOcrRequest("existing image text")
                ),
                new ImageRegisterItemRequest(
                        invalidRequestId,
                        "unsupported.txt",
                        "c".repeat(64),
                        300,
                        new ImageRegisterOcrRequest("unsupported image text")
                )
        ));

        var response = imageCommandService.register(1, request);

        assertThat(response.registered()).singleElement().satisfies(item -> {
            assertThat(item.clientRequestId()).isEqualTo(newRequestId);
            assertThat(item.imageId()).isEqualTo(21);
            assertThat(item.presignedUrl()).isEqualTo("https://storage.example/new-image");
        });
        assertThat(response.duplicated()).singleElement().satisfies(item -> {
            assertThat(item.clientRequestId()).isEqualTo(duplicateRequestId);
            assertThat(item.existingImageId()).isEqualTo(20);
        });
        assertThat(response.failed()).singleElement().satisfies(item -> {
            assertThat(item.clientRequestId()).isEqualTo(invalidRequestId);
            assertThat(item.reason()).isEqualTo("UNSUPPORTED_FORMAT");
        });
    }
}
