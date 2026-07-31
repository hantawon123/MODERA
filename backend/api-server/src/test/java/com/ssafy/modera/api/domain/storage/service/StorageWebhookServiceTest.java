package com.ssafy.modera.api.domain.storage.service;

import com.ssafy.modera.api.domain.event.EventPublisher;
import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.entity.ImageRegistrationRequest;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.ImageRegistrationRequestRepository;
import com.ssafy.modera.api.domain.image.repository.OcrRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.storage.dto.MinioWebhookEvent;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.ImageUploadedPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageWebhookServiceTest {

    @Mock ImageAssetRepository imageAssetRepository;
    @Mock OcrRepository ocrRepository;
    @Mock ImageQueryRepository imageQueryRepository;
    @Mock ImageRegistrationRequestRepository registrationRequestRepository;
    @Mock UserImageRepository userImageRepository;
    @Mock EventPublisher eventPublisher;
    @InjectMocks StorageWebhookService storageWebhookService;

    @Test
    void retriesAnalysisForUploadedAssetWhoseViewIsNone() {
        stubUploadedAssetAndOwner(false);
        when(ocrRepository.findByImageId(7)).thenReturn(Optional.empty());

        storageWebhookService.handle(webhook());

        verify(imageQueryRepository).markUploadProcessing(6, 7);
        verify(eventPublisher).publish(
                eq(Streams.IMAGE_ANALYSIS),
                eq(EventTypes.IMAGE_UPLOADED),
                eq(1),
                any(ImageUploadedPayload.class));
    }

    @Test
    void publishesReuploadWhenDeletedImageHasNoReusableView() {
        stubUploadedAssetAndOwner(false);
        when(userImageRepository.existsByUserIdAndImageIdAndDelYn(6, 7, "Y"))
                .thenReturn(true);
        when(ocrRepository.findByImageId(7)).thenReturn(Optional.empty());

        storageWebhookService.handle(webhook());

        verify(imageQueryRepository).markUploadProcessing(6, 7);
        verify(eventPublisher).publish(
                eq(Streams.IMAGE_ANALYSIS),
                eq(EventTypes.IMAGE_REUPLOAD),
                eq(1),
                any(ImageUploadedPayload.class));
    }

    @Test
    void ignoresWebhookWhenAnalysisIsAlreadyActiveOrCompleted() {
        stubUploadedAssetAndOwner(true);

        storageWebhookService.handle(webhook());

        verify(imageQueryRepository, never()).markUploadProcessing(any(), any());
        verify(eventPublisher, never()).publish(any(), any(), any(Integer.class), any());
    }

    @Test
    void restoredCompletedReuploadDoesNotPublishAnyAnalysisEvent() {
        stubUploadedAssetAndOwner(true);
        when(userImageRepository.existsByUserIdAndImageIdAndDelYn(6, 7, "Y"))
                .thenReturn(true);

        storageWebhookService.handle(webhook());

        verify(imageQueryRepository, never()).markUploadProcessing(any(), any());
        verify(eventPublisher, never()).publish(any(), any(), any(Integer.class), any());
    }

    private void stubUploadedAssetAndOwner(boolean activeOrCompleted) {
        ImageAsset asset = ImageAsset.builder()
                .imageId(7)
                .fileName("60.jpg")
                .contentHash("a".repeat(64))
                .fileSize(200)
                .s3Key("6/7-60.jpg")
                .uploadStatus("UPLOADED")
                .build();
        ImageRegistrationRequest request = new ImageRegistrationRequest(
                6, UUID.randomUUID(), OffsetDateTime.now());
        request.complete(7, false, OffsetDateTime.now());

        when(imageAssetRepository.findByS3KeyAndDelYn("6/7-60.jpg", "N"))
                .thenReturn(Optional.of(asset));
        when(registrationRequestRepository
                .findFirstByImageIdAndStatusAndDelYnOrderByUpdatedAtDesc(
                        7, "REGISTERED", "N"))
                .thenReturn(Optional.of(request));
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(6, 7, "N"))
                .thenReturn(Optional.of(
                        UserImage.builder().userId(6).imageId(7).build()));
        when(imageQueryRepository.isAnalysisActiveOrCompleted(6, 7))
                .thenReturn(activeOrCompleted);
    }

    private MinioWebhookEvent webhook() {
        return new MinioWebhookEvent(List.of(
                new MinioWebhookEvent.Record(
                        new MinioWebhookEvent.S3(
                                new MinioWebhookEvent.Bucket("pictures"),
                                new MinioWebhookEvent.S3Object("6/7-60.jpg")
                        )
                )
        ));
    }
}
