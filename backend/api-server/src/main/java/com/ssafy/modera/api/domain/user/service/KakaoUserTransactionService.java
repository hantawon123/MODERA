package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.dto.response.TokenResponse;
import com.ssafy.modera.api.domain.user.entity.User;
import com.ssafy.modera.api.domain.user.exception.UserErrorCode;
import com.ssafy.modera.api.domain.user.repository.UserRepository;
import com.ssafy.modera.api.domain.user.repository.UserSettingCommandRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.security.jwt.JwtProperties;
import com.ssafy.modera.api.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoUserTransactionService {

    private static final String PROVIDER_KAKAO = "KAKAO";
    private final UserRepository userRepository;
    private final UserSettingCommandRepository userSettingCommandRepository;
    private final RefreshTokenCommandService refreshTokenCommandService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional
    public TokenResponse login(String providerId, String email, String deviceId) {
        User user = userRepository.findByProviderAndProviderId(PROVIDER_KAKAO, providerId)
                .orElseGet(() -> createUser(providerId, email));
        synchronizeEmail(user, email);

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), deviceId);
        refreshTokenCommandService.upsert(
                user.getUserId(),
                deviceId,
                RefreshTokenHash.sha256(refreshToken),
                OffsetDateTime.now().plusSeconds(jwtProperties.getRefreshTokenValidityInSeconds())
        );

        log.debug("로그인 성공: provider={}, userId={}, deviceId={}",
                user.getProvider(), user.getUserId(), deviceId);
        return new TokenResponse(accessToken, refreshToken, user.getUserId());
    }

    private User createUser(String providerId, String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        User user = userRepository.save(User.builder()
                .provider(PROVIDER_KAKAO)
                .providerId(providerId)
                .email(email)
                .updatedAt(OffsetDateTime.now())
                .build());
        userSettingCommandRepository.createDefaults(user.getUserId());
        return user;
    }

    private void synchronizeEmail(User user, String email) {
        if (email.equals(user.getEmail())) {
            return;
        }

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        user.updateEmail(email, OffsetDateTime.now());
        log.info("카카오 이메일 동기화 완료: userId={}", user.getUserId());
    }
}
