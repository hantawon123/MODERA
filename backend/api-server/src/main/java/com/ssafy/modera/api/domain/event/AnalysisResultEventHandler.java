package com.ssafy.modera.api.domain.event;

import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.query.repository.ImageSearchDocumentRepository;
import com.ssafy.modera.api.domain.query.repository.ImageSearchDocumentRow;
import com.ssafy.modera.api.domain.query.repository.UserImageViewRepository;
import com.ssafy.modera.api.domain.query.repository.UserImageViewRow;
import com.ssafy.modera.api.domain.user.entity.User;
import com.ssafy.modera.api.domain.user.repository.UserRepository;
import com.ssafy.modera.contract.payload.AnalysisCompletedPayload;
import com.ssafy.modera.contract.payload.AnalysisFailedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

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
    private final UserRepository userRepository;
    private final UserImageViewRepository userImageViewRepository;
    private final ImageSearchDocumentRepository imageSearchDocumentRepository;

    @Transactional
    public void handleCompleted(AnalysisCompletedPayload payload) {
        Integer imageId = payload.imageId();

        UserImage userImage = userImageRepository.findById(imageId).orElse(null);
        if (userImage == null) {
            log.warn("ANALYSIS_COMPLETED 수신했지만 user_image가 없다: imageId={}", imageId);
            return;
        }
        ImageAsset imageAsset = imageAssetRepository.findById(imageId).orElse(null);
        if (imageAsset == null) {
            log.warn("ANALYSIS_COMPLETED 수신했지만 image_asset이 없다: imageId={}", imageId);
            return;
        }

        String nickname = userRepository.findById(payload.userId()).map(User::getNickname).orElse(null);

        OffsetDateTime now = OffsetDateTime.now();
        userImage.applyAnalysisStatus(payload.analysisStatus(), now);
        userImageRepository.save(userImage);

        userImageViewRepository.upsert(new UserImageViewRow(
                payload.userId(),
                imageId,
                nickname,
                imageAsset.getFileName(),
                imageAsset.getS3Key(),
                imageAsset.getThumbnailKey(),
                userImage.getTitle(),
                payload.summary(),
                payload.categoryName(),
                payload.tagNames(),
                imageAsset.getUploadStatus(),
                payload.analysisStatus(),
                userImage.getFavorite(),
                userImage.getCreatedAt().toInstant(),
                now.toInstant()
        ));

        imageSearchDocumentRepository.upsert(new ImageSearchDocumentRow(
                imageId,
                payload.userId(),
                userImage.getTitle(),
                payload.summary(),
                payload.ocrText(),
                payload.categoryName(),
                payload.tagNames(),
                payload.structuredFields(),
                now.toInstant()
        ));
    }

    @Transactional
    public void handleFailed(AnalysisFailedPayload payload) {
        Integer imageId = payload.imageId();
        UserImage userImage = userImageRepository.findById(imageId).orElse(null);
        if (userImage == null) {
            log.warn("ANALYSIS_FAILED 수신했지만 user_image가 없다: imageId={}", imageId);
            return;
        }
        userImage.applyAnalysisStatus("FAILED", OffsetDateTime.now());
        userImageRepository.save(userImage);
    }
}
