package com.ssafy.modera.api.domain.event;

import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.image.repository.ThumbnailRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.query.repository.UserImageViewRepository;
import com.ssafy.modera.api.domain.query.repository.UserImageViewRow;
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
    private final UserImageViewRepository userImageViewRepository;

    @Transactional
    public void handleCompleted(AnalysisCompletedPayload payload) {
        Integer imageId = payload.imageId();

        UserImage userImage = userImageRepository.findByUserIdAndImageId(payload.userId(), imageId).orElse(null);
        if (userImage == null) {
            log.warn("ANALYSIS_COMPLETED 수신했지만 user_image가 없다: imageId={}", imageId);
            return;
        }
        ImageAsset imageAsset = imageAssetRepository.findById(imageId).orElse(null);
        if (imageAsset == null) {
            log.warn("ANALYSIS_COMPLETED 수신했지만 image_asset이 없다: imageId={}", imageId);
            return;
        }

        userImageViewRepository.upsert(new UserImageViewRow(
                payload.userId(),
                imageId,
                imageAsset.getFileName(),
                imageAsset.getS3Key(),
                thumbnailRepository.findByImageId(imageId)
                        .map(thumbnail -> thumbnail.getS3Key())
                        .orElse(null),
                imageAsset.getFileName(),
                payload.summary(),
                payload.categoryName(),
                payload.tagNames(),
                java.util.List.of(),
                payload.structuredFields(),
                imageAsset.getUploadStatus(),
                payload.analysisStatus(),
                false,
                imageAsset.getCreatedAt().toInstant()
        ));
    }

    @Transactional
    public void handleFailed(AnalysisFailedPayload payload) {
        Integer imageId = payload.imageId();
        UserImage userImage = userImageRepository.findByUserIdAndImageId(payload.userId(), imageId).orElse(null);
        if (userImage == null) {
            log.warn("ANALYSIS_FAILED 수신했지만 user_image가 없다: imageId={}", imageId);
            return;
        }
        userImageViewRepository.updateAnalysisStatus(
                payload.userId(),
                imageId,
                "FAILED"
        );
    }
}
