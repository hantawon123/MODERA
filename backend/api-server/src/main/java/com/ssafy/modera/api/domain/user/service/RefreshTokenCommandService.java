package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.repository.RefreshTokenCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class RefreshTokenCommandService {

    private final RefreshTokenCommandRepository refreshTokenCommandRepository;

    @Transactional
    public void upsert(Integer userId, String deviceId, String tokenHash, OffsetDateTime expiresAt) {
        refreshTokenCommandRepository.upsert(userId, deviceId, tokenHash, expiresAt);
    }
}
