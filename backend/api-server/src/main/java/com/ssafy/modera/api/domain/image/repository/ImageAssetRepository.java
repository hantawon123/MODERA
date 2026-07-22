package com.ssafy.modera.api.domain.image.repository;

import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, UUID> {

    Optional<ImageAsset> findByS3Key(String s3Key);
}
