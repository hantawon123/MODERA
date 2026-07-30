package com.ssafy.modera.api.domain.library.repository;

import com.ssafy.modera.api.domain.library.entity.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserImageRepository extends JpaRepository<UserImage, Integer> {

    Optional<UserImage> findByUserIdAndImageIdAndDelYn(Integer userId, Integer imageId, String delYn);

    Optional<UserImage> findFirstByImageIdAndDelYnOrderByUserImageIdAsc(Integer imageId, String delYn);
}
