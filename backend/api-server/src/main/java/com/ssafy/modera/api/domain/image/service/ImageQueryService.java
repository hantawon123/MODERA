package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.dto.ImageDetailResponse;
import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.domain.query.repository.UserImageViewDetail;
import com.ssafy.modera.api.domain.query.repository.UserImageViewRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageQueryService {

    private final UserImageRepository userImageRepository;
    private final ImageAssetRepository imageAssetRepository;
    private final UserImageViewRepository userImageViewRepository;

    public ImageDetailResponse getImage(Integer userId, Integer imageId) {
        // 다른 사용자 소유 이미지는 403이 아니라 404로 응답한다(존재 여부 자체를 숨긴다).
        userImageRepository.findByUserIdAndImageId(userId, imageId)
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        ImageAsset imageAsset = imageAssetRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        UserImageViewDetail detail = userImageViewRepository
                .findDetail(userId, imageId)
                .orElse(null);

        return new ImageDetailResponse(
                imageId,
                imageAsset.getS3Key(),
                imageAsset.getUploadStatus(),
                detail == null || detail.analysisStatus() == null
                        ? "NONE"
                        : detail.analysisStatus(),
                detail == null || detail.title() == null
                        ? imageAsset.getFileName()
                        : detail.title(),
                detail != null && Boolean.TRUE.equals(detail.favorite()),
                detail == null || detail.createdAt() == null
                        ? imageAsset.getCreatedAt()
                        : detail.createdAt()
        );
    }
}
