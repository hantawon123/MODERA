package com.ssafy.modera.domain.user.repository;

import com.ssafy.modera.domain.user.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByUser_UserIdAndDeviceId(Long userId, String deviceId);
}
