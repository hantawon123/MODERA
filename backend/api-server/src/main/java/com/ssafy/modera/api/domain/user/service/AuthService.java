package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.dto.request.LoginRequest;
import com.ssafy.modera.api.domain.user.dto.request.KakaoLoginRequest;
import com.ssafy.modera.api.domain.user.dto.request.LogoutRequest;
import com.ssafy.modera.api.domain.user.dto.request.RefreshRequest;
import com.ssafy.modera.api.domain.user.dto.request.RegisterRequest;
import com.ssafy.modera.api.domain.user.dto.response.LogoutResponse;
import com.ssafy.modera.api.domain.user.dto.response.RegisterResponse;
import com.ssafy.modera.api.domain.user.dto.response.TokenResponse;
import com.ssafy.modera.api.domain.user.entity.RefreshToken;
import com.ssafy.modera.api.domain.user.entity.User;
import com.ssafy.modera.api.domain.user.exception.UserErrorCode;
import com.ssafy.modera.api.domain.user.repository.RefreshTokenRepository;
import com.ssafy.modera.api.domain.user.repository.UserRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.security.jwt.JwtProperties;
import com.ssafy.modera.api.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;

/**
 * 회원가입 / 로그인 / 토큰 재발급(RTR) / 로그아웃.
 * 비밀번호·토큰 원문은 어떤 로그에도 남기지 않는다 — userId/deviceId 등 식별자만 로그에 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String PROVIDER_LOCAL = "LOCAL";
    private static final String PROVIDER_KAKAO = "KAKAO";
    private static final String HASH_ALGORITHM = "SHA-256";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final KakaoClient kakaoClient;
    private final NicknameGenerator nicknameGenerator;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        OffsetDateTime now = OffsetDateTime.now();
        User user = User.builder()
                .provider(PROVIDER_LOCAL)
                .loginId(request.loginId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .email(request.email())
                .nickname(request.nickname())
                .createdAt(now)
                .updatedAt(now)
                .build();
        userRepository.save(user);

        log.info("회원가입 완료: userId={}", user.getUserId());
        return new RegisterResponse(user.getUserId());
    }

    /** loginId 미존재와 비밀번호 불일치를 구분하지 않고 LOGIN_FAILED로 통합한다(계정 열거 방지). */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.LOGIN_FAILED));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(UserErrorCode.LOGIN_FAILED);
        }

        return issueTokens(user, request.deviceId());
    }

    @Transactional
    public TokenResponse kakaoLogin(KakaoLoginRequest request) {
        KakaoClient.KakaoUser kakaoUser = kakaoClient.getUser(request.authorizationCode());
        String providerId = kakaoUser.id().toString();

        User user = userRepository.findByProviderAndProviderId(PROVIDER_KAKAO, providerId)
                .orElseGet(() -> createKakaoUser(kakaoUser, providerId));

        return issueTokens(user, request.deviceId());
    }

    /**
     * 제시된 refreshToken은 즉시 폐기하고 새 토큰으로 회전한다(RTR). 서명이 무효거나
     * 만료됐거나, 이미 한 번 회전되어 DB의 현재 해시와 더는 일치하지 않는 옛 토큰을
     * 재사용하는 경우 전부 INVALID_REFRESH_TOKEN으로 통합한다.
     */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.isValid(request.refreshToken())) {
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(request.refreshToken()))
                .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN));

        if (!stored.getDeviceId().equals(request.deviceId())) {
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (stored.isExpired(OffsetDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        Integer userId = stored.getUserId();
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String rotatedRefreshToken = jwtTokenProvider.createRefreshToken(userId, stored.getDeviceId());

        stored.rotate(hash(rotatedRefreshToken), refreshTokenExpiry());

        log.info("토큰 재발급 완료: userId={}, deviceId={}", userId, stored.getDeviceId());
        return new TokenResponse(accessToken, rotatedRefreshToken, userId);
    }

    /** 인증된 사용자의 해당 기기 refreshToken만 폐기한다. 다른 기기 토큰으로는 로그아웃할 수 없다. */
    @Transactional
    public LogoutResponse logout(Integer userId, LogoutRequest request) {
        RefreshToken stored = refreshTokenRepository.findByUserIdAndDeviceId(userId, request.deviceId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN));

        if (!stored.matches(hash(request.refreshToken()))) {
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        refreshTokenRepository.delete(stored);

        log.info("로그아웃 완료: userId={}, deviceId={}", userId, request.deviceId());
        return LogoutResponse.success();
    }

    private void upsertRefreshToken(Integer userId, String deviceId, String refreshToken) {
        refreshTokenRepository.findByUserIdAndDeviceId(userId, deviceId)
                .ifPresentOrElse(
                        existing -> existing.rotate(hash(refreshToken), refreshTokenExpiry()),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .userId(userId)
                                .deviceId(deviceId)
                                .tokenHash(hash(refreshToken))
                                .expiresAt(refreshTokenExpiry())
                                .createdAt(OffsetDateTime.now())
                                .build())
                );
    }

    private User createKakaoUser(KakaoClient.KakaoUser kakaoUser, String providerId) {
        OffsetDateTime now = OffsetDateTime.now();
        String email = kakaoUser.email();
        if (email != null && userRepository.existsByEmail(email)) {
            // 이메일은 카카오 회원의 영구 식별자가 아니므로 기존 계정을 자동 연결하지 않는다.
            email = null;
        }

        return userRepository.save(User.builder()
                .provider(PROVIDER_KAKAO)
                .providerId(providerId)
                .email(email)
                .nickname(nicknameGenerator.generateUnique())
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private TokenResponse issueTokens(User user, String deviceId) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), deviceId);
        upsertRefreshToken(user.getUserId(), deviceId, refreshToken);

        log.info("로그인 성공: provider={}, userId={}, deviceId={}",
                user.getProvider(), user.getUserId(), deviceId);
        return new TokenResponse(accessToken, refreshToken, user.getUserId());
    }

    private OffsetDateTime refreshTokenExpiry() {
        return OffsetDateTime.now().plusSeconds(jwtProperties.getRefreshTokenValidityInSeconds());
    }

    /** refresh_token.token_hash는 원문이 아닌 SHA-256 hex(64자)로 저장한다(원문 저장 금지). */
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
