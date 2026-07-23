package com.ssafy.modera.api.domain.library.repository;

import com.ssafy.modera.api.domain.library.entity.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserImageRepository extends JpaRepository<UserImage, UUID> {

    Optional<UserImage> findByUserIdAndClientRequestId(Long userId, String clientRequestId);
}
