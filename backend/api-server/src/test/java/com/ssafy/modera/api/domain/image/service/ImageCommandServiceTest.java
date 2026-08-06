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
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeOutboxService;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeResource;
import org.junit.jupiter.api.DisplayName;
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

import com.ssafy.modera.api.domain.image.repository.UserImageViewDetail;
import com.ssafy.modera.api.domain.schedule.service.ScheduleCreationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Spy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

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
    @Mock UserDataChangeOutboxService userDataChangeOutboxService;
    @Mock ScheduleCreationService scheduleCreationService;
    // 실제 파싱이 필요해 목이 아니라 실 인스턴스를 넣는다(구조화 데이터 JSON을 읽는다).
    @Spy ObjectMapper objectMapper = new ObjectMapper();
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
        when(imageQueryRepository.copyExistingView(1, 20)).thenReturn(true);

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
        verify(userDataChangeOutboxService, never()).record(
                1, UserDataChangeResource.IMAGE, "21");
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
        verify(userDataChangeOutboxService).record(
                2, UserDataChangeResource.IMAGE, "20");
    }

    @Test
    void reissuesRegisteredUploadForADeletedImageWithoutReusableView()
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
                .thenReturn(Optional.empty());
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
        verify(userDataChangeOutboxService, never()).record(
                6, UserDataChangeResource.IMAGE, "7");
    }

    @Test
    void deletesMultipleImagesAndContinuesAfterItemFailure() {
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(imageCommandRepository.deleteImage(1, 10)).thenReturn(ImageDeleteStatus.DELETED);
        when(imageCommandRepository.deleteImage(1, 11)).thenReturn(ImageDeleteStatus.ALREADY_DELETED);
        when(imageCommandRepository.deleteImage(1, 12)).thenReturn(ImageDeleteStatus.NOT_FOUND);
        when(imageCommandRepository.deleteImage(1, 13)).thenThrow(new RuntimeException("database error"));
        when(imageCommandRepository.deleteImage(1, 14)).thenReturn(ImageDeleteStatus.DELETED);

        var response = imageCommandService.deleteImages(
                1,
                new ImageDeleteRequest(List.of(10, 11, 12, 13, 14, 10))
        );

        assertThat(response.deletedImageIds()).containsExactly(10, 14);
        assertThat(response.alreadyDeletedImageIds()).containsExactly(11);
        assertThat(response.failed()).extracting(
                item -> item.imageId() + ":" + item.reason()
        ).containsExactly(
                "12:IMAGE_NOT_FOUND",
                "13:INTERNAL_ERROR"
        );
        assertThat(response.deletedCount()).isEqualTo(2);
        assertThat(response.failedCount()).isEqualTo(2);
        verify(imageCommandRepository, times(1)).deleteImage(1, 10);
        verify(userDataChangeOutboxService).record(
                1, UserDataChangeResource.IMAGE_DELETE_BATCH, "[10,14]");
    }

    @Test
    @DisplayName("중복 업로드로 분석을 물려받으면 두 번째 사용자에게도 일정을 만든다")
    void createsScheduleForUserWhoInheritedAnalysis() throws Exception {
        givenDuplicateUploadOf(20, "b".repeat(64), 200);
        // 복사된 read model에 일정 구조화 데이터가 들어 있는 상황
        when(imageQueryRepository.findDetail(1, 20)).thenReturn(Optional.of(detailWithStructuredData(
                "구름 딥다이브 공모전",
                "{\"type\":\"schedule\",\"fields\":{\"startYear\":2026,\"startMonth\":9,\"startDay\":1}}")));

        imageCommandService.register(1, requestOf("existing.jpg", "b".repeat(64), 200));

        // type과 fields를 나눠 넘겨야 한다(createFromAnalysis 계약).
        verify(scheduleCreationService).createFromAnalysis(
                eq(1), eq(20), eq("구름 딥다이브 공모전"), eq("schedule"),
                argThat(fields -> fields != null && fields.contains("startYear")));
    }

    @Test
    @DisplayName("구조화 데이터가 없는 중복 이미지는 일정을 만들지 않는다")
    void skipsScheduleWhenNoStructuredData() throws Exception {
        givenDuplicateUploadOf(20, "b".repeat(64), 200);
        when(imageQueryRepository.findDetail(1, 20))
                .thenReturn(Optional.of(detailWithStructuredData("제목만 있는 이미지", null)));

        imageCommandService.register(1, requestOf("existing.jpg", "b".repeat(64), 200));

        verify(scheduleCreationService, never()).createFromAnalysis(
                any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("일정 생성이 실패해도 이미지 등록은 성공한다")
    void keepsRegistrationSuccessfulWhenScheduleCreationFails() throws Exception {
        givenDuplicateUploadOf(20, "b".repeat(64), 200);
        when(imageQueryRepository.findDetail(1, 20)).thenReturn(Optional.of(detailWithStructuredData(
                "일정 이미지", "{\"type\":\"schedule\",\"fields\":{}}")));
        org.mockito.Mockito.doThrow(new IllegalStateException("일정 저장 실패"))
                .when(scheduleCreationService).createFromAnalysis(any(), any(), any(), any(), any());

        var response = imageCommandService.register(1, requestOf("existing.jpg", "b".repeat(64), 200));

        assertThat(response.duplicated()).singleElement()
                .satisfies(item -> assertThat(item.existingImageId()).isEqualTo(20));
    }

    /** 이미 소유한 이미지를 같은 해시로 다시 올리는 상황(=duplicated 응답 경로)을 만든다. */
    private void givenDuplicateUploadOf(Integer imageId, String hash, Integer fileSize) {
        ImageAsset existing = ImageAsset.builder()
                .imageId(imageId)
                .fileName("existing.jpg")
                .contentHash(hash)
                .fileSize(fileSize)
                .s3Key("1/" + imageId + "-existing.jpg")
                .uploadStatus("UPLOADED")
                .build();
        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
        when(registrationRequestRepository.findByUserIdAndClientRequestIdAndDelYn(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(registrationRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageAssetRepository.findByContentHashAndDelYn(hash, "N")).thenReturn(Optional.of(existing));
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(1, imageId, "N"))
                .thenReturn(Optional.of(UserImage.builder().userId(1).imageId(imageId).build()));
        when(imageQueryRepository.copyExistingView(1, imageId)).thenReturn(true);
    }

    private ImageRegisterRequest requestOf(String fileName, String hash, Integer fileSize) {
        return new ImageRegisterRequest(List.of(new ImageRegisterItemRequest(
                UUID.randomUUID(), fileName, hash, fileSize,
                new ImageRegisterOcrRequest("text"))));
    }

    private UserImageViewDetail detailWithStructuredData(String title, String structuredDataJson) {
        return new UserImageViewDetail(
                "1/20-existing.jpg", null, "UPLOADED", "COMPLETED", title, false, "요약",
                null, "일정", List.of(), List.of(), structuredDataJson, null, null, false, false);
    }
}
