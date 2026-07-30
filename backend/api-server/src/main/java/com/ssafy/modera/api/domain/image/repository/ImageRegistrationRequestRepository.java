package com.ssafy.modera.api.domain.image.repository;

import com.ssafy.modera.api.domain.image.entity.ImageRegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImageRegistrationRequestRepository extends JpaRepository<ImageRegistrationRequest, Integer> {
    Optional<ImageRegistrationRequest> findByUserIdAndClientRequestIdAndDelYn(
            Integer userId,
            UUID clientRequestId,
            String delYn
    );

    Optional<ImageRegistrationRequest> findFirstByImageIdAndStatusAndDelYnOrderByUpdatedAtDesc(
            Integer imageId,
            String status,
            String delYn
    );
}
