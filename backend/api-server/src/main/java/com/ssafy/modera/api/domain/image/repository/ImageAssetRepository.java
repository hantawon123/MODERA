package com.ssafy.modera.api.domain.image.repository;

import com.ssafy.modera.api.domain.image.entity.ImageAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, Integer> {

    Optional<ImageAsset> findByS3KeyAndDelYn(String s3Key, String delYn);

    Optional<ImageAsset> findByContentHashAndDelYn(String contentHash, String delYn);

    Optional<ImageAsset> findByImageIdAndDelYn(Integer imageId, String delYn);

    @Query(value = "SELECT nextval('image_schema.image_asset_image_id_seq')", nativeQuery = true)
    Integer nextImageId();
}
