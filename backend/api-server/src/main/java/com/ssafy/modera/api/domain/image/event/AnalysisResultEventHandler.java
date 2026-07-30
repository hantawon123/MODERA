package com.ssafy.modera.api.domain.image.event;

import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.image.repository.ThumbnailRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.image.repository.ImageQueryRepository;
import com.ssafy.modera.api.domain.image.repository.UserImageViewRow;
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

        imageQueryRepository.upsert(new UserImageViewRow(
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
                false
        ));
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
    }
}
