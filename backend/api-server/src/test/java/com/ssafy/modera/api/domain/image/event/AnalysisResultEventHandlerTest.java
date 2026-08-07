package com.ssafy.modera.api.domain.image.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.ThumbnailRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeOutboxService;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeResource;
import com.ssafy.modera.api.domain.schedule.service.ScheduleCreationService;
import com.ssafy.modera.contract.payload.AnalysisCompletedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisResultEventHandlerTest {

    @Mock UserImageRepository userImageRepository;
    @Mock ImageAssetRepository imageAssetRepository;
    @Mock ThumbnailRepository thumbnailRepository;
    @Mock ImageQueryRepository imageQueryRepository;
    @Mock ScheduleCreationService scheduleCreationService;
    @Mock UserDataChangeOutboxService userDataChangeOutboxService;

    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks AnalysisResultEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new AnalysisResultEventHandler(
                userImageRepository,
                imageAssetRepository,
                thumbnailRepository,
                imageQueryRepository,
                scheduleCreationService,
                objectMapper,
                userDataChangeOutboxService
        );
        when(userImageRepository.findByUserIdAndImageIdAndDelYn(7, 18, "N"))
                .thenReturn(Optional.of(UserImage.builder()
                        .userId(7).imageId(18).build()));
        when(imageAssetRepository.findByImageIdAndDelYn(18, "N"))
                .thenReturn(Optional.of(ImageAsset.builder()
                        .imageId(18)
                        .fileName("image.jpg")
                        .s3Key("7/18-image.jpg")
                        .uploadStatus("UPLOADED")
                        .build()));
    }

    @Test
    void defersNotificationUntilInitialCategoryIsStored() {
        eventHandler.handleCompleted(completed("자동차"));

        verify(userDataChangeOutboxService, never()).record(
                7, UserDataChangeResource.IMAGE_UPLOAD, "18");
    }

    @Test
    void notifiesCategorylessAnalysisExactlyOnce() {
        eventHandler.handleCompleted(completed(null));

        verify(userDataChangeOutboxService).record(
                7, UserDataChangeResource.IMAGE_UPLOAD, "18");
    }

    @Test
    void routesReuploadAnalysisToReanalysisResource() {
        eventHandler.handleCompleted(completed(null, "REUPLOAD"));

        verify(userDataChangeOutboxService).record(
                7, UserDataChangeResource.IMAGE_REANALYSIS, "18");
    }

    private AnalysisCompletedPayload completed(String categoryName) {
        return completed(categoryName, "INITIAL");
    }

    private AnalysisCompletedPayload completed(String categoryName, String triggerType) {
        return new AnalysisCompletedPayload(
                18,
                7,
                "title",
                "summary",
                "ocr",
                null,
                categoryName,
                List.of("tag"),
                List.of("key"),
                null,
                null,
                "COMPLETED",
                "model-v1",
                triggerType
        );
    }
}
