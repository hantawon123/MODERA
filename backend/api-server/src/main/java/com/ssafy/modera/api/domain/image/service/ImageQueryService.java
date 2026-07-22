package com.ssafy.modera.api.domain.image.service;

import com.ssafy.modera.api.domain.image.dto.ImageDetailResponse;
import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import com.ssafy.modera.api.domain.image.repository.ImageAssetRepository;
import com.ssafy.modera.api.domain.library.entity.UserImage;
import com.ssafy.modera.api.domain.library.repository.UserImageRepository;
import com.ssafy.modera.api.domain.image.exception.ImageErrorCode;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageQueryService {

    private final UserImageRepository userImageRepository;
    private final ImageAssetRepository imageAssetRepository;

    public ImageDetailResponse getImage(Long userId, UUID imageId) {
        // 다른 사용자 소유 이미지는 403이 아니라 404로 응답한다(존재 여부 자체를 숨긴다).
        UserImage userImage = userImageRepository.findById(imageId)
                .filter(image -> image.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        ImageAsset imageAsset = imageAssetRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ImageErrorCode.IMAGE_NOT_FOUND));

        return new ImageDetailResponse(
                imageId,
                imageAsset.getS3Key(),
                imageAsset.getUploadStatus(),
                userImage.getAnalysisStatus(),
                userImage.getTitle(),
                userImage.getFavorite(),
                userImage.getCreatedAt()
        );
    }
}
