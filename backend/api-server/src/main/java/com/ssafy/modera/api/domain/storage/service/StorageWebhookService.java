package com.ssafy.modera.api.domain.storage.service;

import com.ssafy.modera.api.domain.event.EventPublisher;
import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.storage.dto.MinioWebhookEvent;
import com.ssafy.modera.contract.EventTypes;
import com.ssafy.modera.contract.Streams;
import com.ssafy.modera.contract.payload.ImageUploadedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageWebhookService {

    private final ImageAssetRepository imageAssetRepository;
    private final UserImageRepository userImageRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void handle(MinioWebhookEvent event) {
        if (event.records() == null) {
            return;
        }

        for (MinioWebhookEvent.Record record : event.records()) {
            handleRecord(record);
        }
    }

    private void handleRecord(MinioWebhookEvent.Record record) {
        if (record.s3() == null || record.s3().object() == null) {
            log.warn("MinIO webhook record에 object 정보가 없어 무시한다");
            return;
        }

        String s3Key = URLDecoder.decode(record.s3().object().key(), StandardCharsets.UTF_8);

        ImageAsset imageAsset = imageAssetRepository.findByS3Key(s3Key).orElse(null);
        if (imageAsset == null) {
            log.warn("s3Key={} 에 해당하는 image_asset을 찾지 못해 webhook을 무시한다", s3Key);
            return;
        }

        UserImage userImage = userImageRepository.findById(imageAsset.getImageId()).orElse(null);
        if (userImage == null) {
            log.warn("imageId={} 에 해당하는 user_image를 찾지 못해 webhook을 무시한다", imageAsset.getImageId());
            return;
        }

        imageAsset.markUploaded(OffsetDateTime.now());
        imageAssetRepository.save(imageAsset);

        // TODO: 클라이언트가 등록 시점에 보낸 OCR 결과를 저장할 곳이 아직 없어 null로 발행한다.
        ImageUploadedPayload payload = new ImageUploadedPayload(
                imageAsset.getImageId(),
                userImage.getUserId(),
                imageAsset.getS3Key(),
                null
        );
        eventPublisher.publish(Streams.IMAGE_ANALYSIS, EventTypes.IMAGE_UPLOADED, 1, payload);

        log.info("IMAGE_UPLOADED 발행: imageId={}, userId={}, s3Key={}",
                imageAsset.getImageId(), userImage.getUserId(), imageAsset.getS3Key());
    }
}
