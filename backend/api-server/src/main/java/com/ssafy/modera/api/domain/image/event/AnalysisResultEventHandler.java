package com.ssafy.modera.api.domain.image.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.image.repository.ThumbnailRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.UserImageViewRow;
import com.ssafy.modera.api.domain.schedule.service.ScheduleCreationService;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeOutboxService;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeResource;
import com.ssafy.modera.contract.payload.AnalysisCompletedPayload;
import com.ssafy.modera.contract.payload.AnalysisFailedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * analysis-result 스트림에서 받은 이벤트를 api-server가 소유한 데이터(library_schema.user_image)와
 * 조회 전용 사본(query_schema.*)에 반영한다. query_schema는 원본이 아니라 이벤트를 합친 read model이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisResultEventHandler {

    private final UserImageRepository userImageRepository;
    private final ImageAssetRepository imageAssetRepository;
    private final ThumbnailRepository thumbnailRepository;
    private final ImageQueryRepository imageQueryRepository;
    private final ScheduleCreationService scheduleCreationService;
    private final ObjectMapper objectMapper;
    private final UserDataChangeOutboxService userDataChangeOutboxService;

    @Transactional
    public void handleCompleted(AnalysisCompletedPayload payload) {
        Integer imageId = payload.imageId();

        UserImage userImage = userImageRepository
                .findByUserIdAndImageIdAndDelYn(payload.userId(), imageId, "N")
                .orElse(null);
        if (userImage == null) {
            log.warn("ANALYSIS_COMPLETED 수신했지만 user_image가 없다: imageId={}", imageId);
            return;
        }
        ImageAsset imageAsset = imageAssetRepository.findByImageIdAndDelYn(imageId, "N").orElse(null);
        if (imageAsset == null) {
            log.warn("ANALYSIS_COMPLETED 수신했지만 image_asset이 없다: imageId={}", imageId);
            return;
        }

        String resolvedTitle = payload.title() == null || payload.title().isBlank()
                ? imageAsset.getFileName()
                : payload.title();
        imageQueryRepository.upsert(new UserImageViewRow(
                payload.userId(),
                imageId,
                imageAsset.getFileName(),
                imageAsset.getS3Key(),
                resolveThumbnailKey(payload, imageId),
                resolvedTitle,
                payload.summary(),
                null,
                payload.categoryName(),
                payload.tagNames(),
                payload.keyInformation(),
                structuredData(payload),
                // AI가 정제한 OCR 텍스트. 원문(image_schema.ocr.content)은 앱이 등록 때 보낸
                // 온디바이스 OCR이라 별개다 — 정제본은 AI 산출물이라 read model에만 둔다.
                payload.ocrText(),
                imageAsset.getUploadStatus(),
                payload.analysisStatus(),
                false,
                imageAsset.getUploadedAt()
        ));

        scheduleCreationService.createFromAnalysis(
                payload.userId(),
                imageId,
                resolvedTitle,
                payload.structuredType(),
                payload.structuredFields()
        );
        // categoryId가 있는 일반 분석은 바로 뒤의 INITIAL_CATEGORY_RESOLVED가
        // 카테고리까지 저장한 뒤 알림을 한 번 기록한다. 카테고리가 없는 EMPTY 등의
        // 결과만 여기서 직접 완료 알림을 기록한다.
        if (payload.categoryName() == null || payload.categoryName().isBlank()) {
            userDataChangeOutboxService.record(
                    payload.userId(), imageAnalysisResource(payload.triggerType()),
                    String.valueOf(imageId));
        }
    }

    private String resolveThumbnailKey(AnalysisCompletedPayload payload, Integer imageId) {
        if (payload.thumbnailKey() != null && !payload.thumbnailKey().isBlank()) {
            return payload.thumbnailKey();
        }
        return thumbnailRepository.findByImageId(imageId)
                .map(thumbnail -> thumbnail.getS3Key())
                .orElse(null);
    }

    private String structuredData(AnalysisCompletedPayload payload) {
        boolean hasType = payload.structuredType() != null && !payload.structuredType().isBlank();
        boolean hasFields = payload.structuredFields() != null && !payload.structuredFields().isBlank();
        if (!hasType && !hasFields) {
            return null;
        }

        ObjectNode structuredData = objectMapper.createObjectNode();
        if (hasType) {
            structuredData.put("type", payload.structuredType());
        }
        if (hasFields) {
            try {
                structuredData.set("fields", objectMapper.readTree(payload.structuredFields()));
            } catch (JsonProcessingException exception) {
                log.warn("구조화 데이터 fields JSON 파싱 실패: imageId={}", payload.imageId(), exception);
            }
        }
        return structuredData.toString();
    }

    @Transactional
    public void handleFailed(AnalysisFailedPayload payload) {
        Integer imageId = payload.imageId();
        UserImage userImage = userImageRepository
                .findByUserIdAndImageIdAndDelYn(payload.userId(), imageId, "N")
                .orElse(null);
        if (userImage == null) {
            log.warn("ANALYSIS_FAILED 수신했지만 user_image가 없다: imageId={}", imageId);
            return;
        }
        imageQueryRepository.updateAnalysisStatus(
                payload.userId(),
                imageId,
                "FAILED"
        );
        userDataChangeOutboxService.record(
                payload.userId(), imageAnalysisResource(payload.triggerType()),
                String.valueOf(imageId));
    }

    private String imageAnalysisResource(String triggerType) {
        return triggerType != null && !"INITIAL".equals(triggerType)
                ? UserDataChangeResource.IMAGE_REANALYSIS
                : UserDataChangeResource.IMAGE_UPLOAD;
    }
}
