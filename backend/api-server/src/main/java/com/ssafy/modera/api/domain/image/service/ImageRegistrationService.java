package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.dto.ImageRegisterRequest;
import com.ssafy.modera.api.domain.image.dto.ImageRegisterResponse;
import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.global.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageRegistrationService {

    private static final Duration PUT_URL_TTL = Duration.ofMinutes(10);

    private final ImageAssetRepository imageAssetRepository;
    private final UserImageRepository userImageRepository;
    private final StorageProperties storageProperties;
    private final S3Presigner s3Presigner;

    @Transactional
    public ImageRegisterResponse register(Long userId, ImageRegisterRequest request) {
        return userImageRepository.findByUserIdAndClientRequestId(userId, request.clientRequestId())
                .map(this::toResponseForExisting)
                .orElseGet(() -> createNew(userId, request));
    }

    private ImageRegisterResponse toResponseForExisting(UserImage userImage) {
        ImageAsset imageAsset = imageAssetRepository.findById(userImage.getImageId())
                .orElseThrow(() -> new IllegalStateException(
                        "user_image는 있는데 image_asset이 없다: imageId=" + userImage.getImageId()));
        return toResponse(imageAsset);
    }

    private ImageRegisterResponse createNew(Long userId, ImageRegisterRequest request) {
        UUID imageId = UUID.randomUUID();
        String s3Key = "%d/%s-%s".formatted(userId, imageId, request.fileName());
        OffsetDateTime now = OffsetDateTime.now();

        ImageAsset imageAsset = ImageAsset.builder()
                .imageId(imageId)
                .fileName(request.fileName())
                .contentHash(request.contentHash())
                .fileSize(request.fileSize())
                .s3Key(s3Key)
                .uploadStatus("PENDING")
                .createdAt(now)
                .updatedAt(now)
                .build();
        imageAssetRepository.save(imageAsset);

        UserImage userImage = UserImage.builder()
                .imageId(imageId)
                .userId(userId)
                .clientRequestId(request.clientRequestId())
                .title(request.fileName())
                .createdAt(now)
                .updatedAt(now)
                .build();
        userImageRepository.save(userImage);

        return toResponse(imageAsset);
    }

    private ImageRegisterResponse toResponse(ImageAsset imageAsset) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket().getPictures())
                .key(imageAsset.getS3Key())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(PUT_URL_TTL)
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        return new ImageRegisterResponse(
                imageAsset.getImageId(),
                toPublicUrl(presigned.url()),
                imageAsset.getS3Key(),
                OffsetDateTime.now().plus(PUT_URL_TTL)
        );
    }

    /**
     * S3Presigner는 항상 S3_INTERNAL_ENDPOINT로 서명하므로, 클라이언트에게 줄 URL은
     * 호스트만 S3_PUBLIC_ENDPOINT로 치환한다(경로·서명 쿼리는 그대로 유지).
     * URI의 다중 인자 생성자를 쓰면 이미 퍼센트 인코딩된 쿼리를 다시 인코딩해버려서
     * (예: %2F → %252F) 서명이 깨지므로, 문자열을 그대로 이어붙인다.
     * 참고: SigV4 서명은 Host를 서명 대상 헤더에 포함하므로, internal/public이
     * 실제로 다른 호스트인 운영 환경에서는 앞단 프록시가 Host를 투명하게 넘겨야
     * 서명 검증이 통과한다(이 부분은 인프라 담당 영역).
     */
    private String toPublicUrl(URL internalUrl) {
        URI publicBase = URI.create(storageProperties.getPublicEndpoint());
        String base = publicBase.getScheme() + "://" + publicBase.getAuthority() + internalUrl.getPath();
        return internalUrl.getQuery() == null ? base : base + "?" + internalUrl.getQuery();
    }
}
