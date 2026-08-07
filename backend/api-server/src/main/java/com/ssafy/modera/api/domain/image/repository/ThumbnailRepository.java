package com.ssafy.modera.api.domain.image.repository;

import com.ssafy.modera.api.domain.image.entity.Thumbnail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThumbnailRepository extends JpaRepository<Thumbnail, Integer> {

    Optional<Thumbnail> findByImageId(Integer imageId);
}
