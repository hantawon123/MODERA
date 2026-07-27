package com.ssafy.modera.api.domain.library.repository;

import com.ssafy.modera.api.domain.library.entity.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserImageRepository extends JpaRepository<UserImage, Integer> {

    Optional<UserImage> findByUserIdAndClientRequestId(Integer userId, String clientRequestId);

    Optional<UserImage> findByUserIdAndImageId(Integer userId, Integer imageId);

    Optional<UserImage> findFirstByImageIdOrderByUserImageIdAsc(Integer imageId);
}
