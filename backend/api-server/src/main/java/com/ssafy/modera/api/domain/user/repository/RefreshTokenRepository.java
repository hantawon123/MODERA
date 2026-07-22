package com.ssafy.modera.api.domain.user.repository;

import com.ssafy.modera.api.domain.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
