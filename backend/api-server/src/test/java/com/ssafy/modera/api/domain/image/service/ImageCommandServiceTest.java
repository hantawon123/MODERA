package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.category.repository.CategoryCommandRepository;
import com.ssafy.modera.api.domain.image.dto.request.ImageDeleteRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageFavoriteRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageRegisterItemRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageRegisterOcrRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageRegisterRequest;
import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.image.repository.ImageCommandRepository;
import com.ssafy.modera.api.domain.image.repository.ImageDeleteStatus;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ImageCommandServiceTest {

    @Mock ImageAssetRepository imageAssetRepository;
    @Mock ImageCommandRepository imageCommandRepository;
    @Mock OcrRepository ocrRepository;
    @Mock ImageRegistrationRequestRepository registrationRequestRepository;
    @Mock UserImageRepository userImageRepository;
    @Mock ImageQueryRepository imageQueryRepository;
    @Mock CategoryCommandRepository categoryCommandRepository;
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
    void updatesFavoriteAndReturnsCurrentFavoriteCount() {
        when(imageCommandRepository.updateFavorite(1, 10, true)).thenReturn(7);

        var response = imageCommandService.updateFavorite(
                1,
                10,
                new ImageFavoriteRequest(true)
        );

        assertThat(response.imageId()).isEqualTo(10);
        assertThat(response.favorite()).isTrue();
        assertThat(response.favoriteCount()).isEqualTo(7);
    }

    @Test
    void hidesMissingFavoriteTargetAsNotFound() {
        when(imageCommandRepository.updateFavorite(1, 10, false)).thenReturn(null);

        assertThatThrownBy(() -> imageCommandService.updateFavorite(
                1,
                10,
                new ImageFavoriteRequest(false)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
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
    void reissuesUploadUrlBeforeUploadCompletes() throws Exception {
        ImageAsset asset = ImageAsset.builder()
                .imageId(10)
                .fileName("image.jpg")
                .contentHash("a".repeat(64))
                .fileSize(100)
                .s3Key("1/10-image.jpg")
                .uploadStatus("PENDING")
                .build();
        StorageProperties.Bucket bucket = new StorageProperties.Bucket();
        bucket.setPictures("pictures");
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(1, 10, "N"))
                .thenReturn(Optional.of(UserImage.builder().userId(1).imageId(10).build()));
        when(imageAssetRepository.findByImageIdAndDelYn(10, "N"))
                .thenReturn(Optional.of(asset));
        when(imageQueryRepository.findDetail(1, 10)).thenReturn(Optional.empty());
        when(storageProperties.getBucket()).thenReturn(bucket);
        PresignedPutObjectRequest presigned =
                org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(
                URI.create("https://storage.example/reissued").toURL()
        );
        when(s3Presigner.presignPutObject(any(
                software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class
        ))).thenReturn(presigned);

        var response = imageCommandService.reissueUploadUrl(1, 10);

        assertThat(response.imageId()).isEqualTo(10);
        assertThat(response.presignedUrl())
                .isEqualTo("https://storage.example/reissued");
        assertThat(response.uploadExpiresIn()).isEqualTo(600);
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
        when(imageQueryRepository.isAnalysisActiveOrCompleted(1, 20))
                .thenReturn(true);
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

    @Test
    void returnsTheExistingObjectPresignedUrlWhenAnotherUserRegistersIt()
            throws Exception {
        String contentHash = "d".repeat(64);
        UUID requestId = UUID.randomUUID();
        ImageAsset sharedAsset = ImageAsset.builder()
                .imageId(20)
                .fileName("shared.jpg")
                .contentHash(contentHash)
                .fileSize(200)
                .s3Key("1/20-shared.jpg")
                .uploadStatus("UPLOADED")
                .build();
        StorageProperties.Bucket bucket = new StorageProperties.Bucket();
        bucket.setPictures("pictures");

        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(storageProperties.getBucket()).thenReturn(bucket);
        when(registrationRequestRepository.findByUserIdAndClientRequestIdAndDelYn(
                2, requestId, "N")).thenReturn(Optional.empty());
        when(registrationRequestRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(imageAssetRepository.findByContentHashAndDelYn(contentHash, "N"))
                .thenReturn(Optional.of(sharedAsset));
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(2, 20, "N"))
                .thenReturn(Optional.empty());
        when(imageQueryRepository.copyExistingView(2, 20)).thenReturn(true);

        PresignedPutObjectRequest presignedRequest =
                org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(
                URI.create("https://storage.example/1/20-shared.jpg").toURL());
        when(s3Presigner.presignPutObject(any(
                software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        var response = imageCommandService.register(
                2,
                new ImageRegisterRequest(List.of(new ImageRegisterItemRequest(
                        requestId,
                        "shared.jpg",
                        contentHash,
                        200,
                        new ImageRegisterOcrRequest("shared image text")
                )))
        );

        assertThat(response.duplicated()).isEmpty();
        assertThat(response.registered()).singleElement().satisfies(item -> {
            assertThat(item.clientRequestId()).isEqualTo(requestId);
            assertThat(item.imageId()).isEqualTo(20);
            assertThat(item.presignedUrl())
                    .isEqualTo("https://storage.example/1/20-shared.jpg");
        });
        verify(userImageRepository).saveAndFlush(any(UserImage.class));
    }

    @Test
    void reissuesRegisteredUploadForAnExistingNoneAnalysisView()
            throws Exception {
        String contentHash = "e".repeat(64);
        UUID requestId = UUID.randomUUID();
        ImageAsset asset = ImageAsset.builder()
                .imageId(7)
                .fileName("60.jpg")
                .contentHash(contentHash)
                .fileSize(200)
                .s3Key("6/7-60.jpg")
                .uploadStatus("UPLOADED")
                .build();
        StorageProperties.Bucket bucket = new StorageProperties.Bucket();
        bucket.setPictures("pictures");

        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(storageProperties.getBucket()).thenReturn(bucket);
        when(registrationRequestRepository.findByUserIdAndClientRequestIdAndDelYn(
                6, requestId, "N")).thenReturn(Optional.empty());
        when(registrationRequestRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(imageAssetRepository.findByContentHashAndDelYn(contentHash, "N"))
                .thenReturn(Optional.of(asset));
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(6, 7, "N"))
                .thenReturn(Optional.of(
                        UserImage.builder().userId(6).imageId(7).build()));
        when(imageQueryRepository.isAnalysisActiveOrCompleted(6, 7))
                .thenReturn(false);
        when(imageQueryRepository.copyExistingView(6, 7)).thenReturn(false);

        PresignedPutObjectRequest presignedRequest =
                org.mockito.Mockito.mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(
                URI.create("https://storage.example/6/7-60.jpg").toURL());
        when(s3Presigner.presignPutObject(any(
                software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        var response = imageCommandService.register(
                6,
                new ImageRegisterRequest(List.of(new ImageRegisterItemRequest(
                        requestId,
                        "60.jpg",
                        contentHash,
                        200,
                        new ImageRegisterOcrRequest("retry")
                )))
        );

        assertThat(response.duplicated()).isEmpty();
        assertThat(response.registered()).singleElement().satisfies(item ->
                assertThat(item.presignedUrl())
                        .isEqualTo("https://storage.example/6/7-60.jpg"));
    }

    @Test
    void deletesMultipleImagesAndContinuesAfterItemFailure() {
        when(imageCommandRepository.deleteImage(1, 10)).thenReturn(ImageDeleteStatus.DELETED);
        when(imageCommandRepository.deleteImage(1, 11)).thenReturn(ImageDeleteStatus.ALREADY_DELETED);
        when(imageCommandRepository.deleteImage(1, 12)).thenReturn(ImageDeleteStatus.NOT_FOUND);
        when(imageCommandRepository.deleteImage(1, 13)).thenThrow(new RuntimeException("database error"));

        var response = imageCommandService.deleteImages(
                1,
                new ImageDeleteRequest(List.of(10, 11, 12, 13, 10))
        );

        assertThat(response.deletedImageIds()).containsExactly(10);
        assertThat(response.alreadyDeletedImageIds()).containsExactly(11);
        assertThat(response.failed()).extracting(
                item -> item.imageId() + ":" + item.reason()
        ).containsExactly(
                "12:IMAGE_NOT_FOUND",
                "13:INTERNAL_ERROR"
        );
        assertThat(response.deletedCount()).isEqualTo(1);
        assertThat(response.failedCount()).isEqualTo(2);
        verify(imageCommandRepository, times(1)).deleteImage(1, 10);
    }
}
