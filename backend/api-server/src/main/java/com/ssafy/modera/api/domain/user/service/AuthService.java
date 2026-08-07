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
import com.ssafy.modera.api.domain.user.repository.UserSettingCommandRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.security.jwt.JwtProperties;
import com.ssafy.modera.api.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * 회원가입 / 로그인 / 토큰 재발급(RTR) / 로그아웃.
 * 비밀번호·토큰 원문은 어떤 로그에도 남기지 않는다 — userId/deviceId 등 식별자만 로그에 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String PROVIDER_LOCAL = "LOCAL";
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginCredentialReader loginCredentialReader;
    private final RefreshTokenCommandService refreshTokenCommandService;
    private final KakaoUserTransactionService kakaoUserTransactionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final KakaoClient kakaoClient;
    private final UserSettingCommandRepository userSettingCommandRepository;

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
                .updatedAt(now)
                .build();
        userRepository.save(user);
        userSettingCommandRepository.createDefaults(user.getUserId());

        log.info("회원가입 완료: userId={}", user.getUserId());
        return new RegisterResponse(user.getUserId());
    }

    /** loginId 미존재와 비밀번호 불일치를 구분하지 않고 LOGIN_FAILED로 통합한다(계정 열거 방지). */
    public TokenResponse login(LoginRequest request) {
        LoginCredentialReader.LoginCredential credential =
                loginCredentialReader.findByLoginId(request.loginId());

        if (credential.passwordHash() == null
                || !passwordEncoder.matches(request.password(), credential.passwordHash())) {
            throw new BusinessException(UserErrorCode.LOGIN_FAILED);
        }

        return issueTokens(credential.userId(), credential.provider(), request.deviceId());
    }

    public TokenResponse kakaoLogin(KakaoLoginRequest request) {
        KakaoClient.KakaoUser kakaoUser = kakaoClient.getVerifiedUser(request.kakaoAccessToken());
        validateKakaoEmail(kakaoUser);
        String providerId = kakaoUser.id().toString();
        String email = normalizeEmail(kakaoUser.email());

        return kakaoUserTransactionService.login(providerId, email, request.deviceId());
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

        RefreshToken stored = refreshTokenRepository.findByTokenHash(RefreshTokenHash.sha256(request.refreshToken()))
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

        stored.rotate(RefreshTokenHash.sha256(rotatedRefreshToken), refreshTokenExpiry());

        log.info("토큰 재발급 완료: userId={}, deviceId={}", userId, stored.getDeviceId());
        return new TokenResponse(accessToken, rotatedRefreshToken, userId);
    }

    /** 인증된 사용자의 해당 기기 refreshToken만 폐기한다. 다른 기기 토큰으로는 로그아웃할 수 없다. */
    @Transactional
    public LogoutResponse logout(Integer userId, LogoutRequest request) {
        RefreshToken stored = refreshTokenRepository.findByUserIdAndDeviceId(userId, request.deviceId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN));

        if (!stored.matches(RefreshTokenHash.sha256(request.refreshToken()))) {
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        refreshTokenRepository.delete(stored);

        log.info("로그아웃 완료: userId={}, deviceId={}", userId, request.deviceId());
        return LogoutResponse.success();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateKakaoEmail(KakaoClient.KakaoUser kakaoUser) {
        if (normalizeEmail(kakaoUser.email()) == null) {
            throw new BusinessException(UserErrorCode.KAKAO_EMAIL_REQUIRED);
        }
        if (!kakaoUser.hasValidVerifiedEmail()) {
            throw new BusinessException(UserErrorCode.KAKAO_EMAIL_NOT_VERIFIED);
        }
    }

    private TokenResponse issueTokens(Integer userId, String provider, String deviceId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, deviceId);
        refreshTokenCommandService.upsert(
                userId, deviceId, RefreshTokenHash.sha256(refreshToken), refreshTokenExpiry());

        log.debug("로그인 성공: provider={}, userId={}, deviceId={}",
                provider, userId, deviceId);
        return new TokenResponse(accessToken, refreshToken, userId);
    }

    private OffsetDateTime refreshTokenExpiry() {
        return OffsetDateTime.now().plusSeconds(jwtProperties.getRefreshTokenValidityInSeconds());
    }

}
