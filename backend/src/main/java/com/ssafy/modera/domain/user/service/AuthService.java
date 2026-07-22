package com.ssafy.modera.domain.user.service;

import com.ssafy.modera.domain.user.dto.request.LoginRequest;
import com.ssafy.modera.domain.user.dto.request.LogoutRequest;
import com.ssafy.modera.domain.user.dto.request.RegisterRequest;
import com.ssafy.modera.domain.user.dto.request.ReissueRequest;
import com.ssafy.modera.domain.user.dto.response.LoginResponse;
import com.ssafy.modera.domain.user.dto.response.LogoutResponse;
import com.ssafy.modera.domain.user.dto.response.RegisterResponse;
import com.ssafy.modera.domain.user.dto.response.ReissueResponse;
import com.ssafy.modera.domain.user.entity.Provider;
import com.ssafy.modera.domain.user.entity.RefreshToken;
import com.ssafy.modera.domain.user.entity.User;
import com.ssafy.modera.domain.user.exception.UserErrorCode;
import com.ssafy.modera.domain.user.repository.RefreshTokenRepository;
import com.ssafy.modera.domain.user.repository.UserRepository;
import com.ssafy.modera.global.exception.AuthorizationException;
import com.ssafy.modera.global.exception.BusinessException;
import com.ssafy.modera.global.security.jwt.JwtProperties;
import com.ssafy.modera.global.security.jwt.JwtTokenProvider;
import com.ssafy.modera.global.security.jwt.JwtTokenValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

/**
 * 인증 API (3-1 회원가입 / 3-2 로그인 / 3-3 토큰 재발급 / 3-4 로그아웃)...
 * <p>
 * ROLE 개념이 없는 서비스이므로 모든 사용자에게 {@link #ROLE_USER} 를 고정 부여한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ROLE_USER = "ROLE_USER";

    /** deviceId 를 보내지 않는 클라이언트(웹 등)를 위한 기본 기기 식별자. */
    private static final String DEFAULT_DEVICE_ID = "default";

    private static final String HASH_ALGORITHM = "SHA-256";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenValidator jwtTokenValidator;
    private final JwtProperties jwtProperties;

    /**
     * 3-1 회원가입. 비밀번호는 bcrypt 로 인코딩해 저장한다.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
        }

        User user = userRepository.save(User.ofLocal(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.email(),
                request.nickname()
        ));

        log.info("회원가입 완료: userId={}", user.getUserId());

        return RegisterResponse.of(user.getUserId());
    }

    /**
     * 3-2 로그인. loginId 미존재와 비밀번호 불일치를 구분하지 않고 LOGIN_FAILED 로 통합한다.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginIdAndProvider(request.loginId(), Provider.LOCAL)
                .orElseThrow(() -> new BusinessException(UserErrorCode.LOGIN_FAILED));

        if (!StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(UserErrorCode.LOGIN_FAILED);
        }

        String deviceId = resolveDeviceId(request.deviceId());
        String accessToken = createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId(), user.getEmail(), deviceId);

        // 같은 기기의 기존 토큰은 새 토큰으로 덮어써 1기기 1토큰을 유지한다. (UNIQUE(user_id, device_id))
        refreshTokenRepository.findByUser_UserIdAndDeviceId(user.getUserId(), deviceId)
                .ifPresentOrElse(
                        existing -> existing.rotate(hash(refreshToken), refreshTokenExpiry()),
                        () -> refreshTokenRepository.save(
                                RefreshToken.issue(user, deviceId, hash(refreshToken), refreshTokenExpiry()))
                );

        log.info("로그인 성공: userId={}, deviceId={}", user.getUserId(), deviceId);

        return LoginResponse.of(accessToken, refreshToken, user.getUserId());
    }

    /**
     * 3-3 토큰 재발급. 제시된 refreshToken 은 즉시 폐기하고 새 토큰으로 회전한다.
     */
    @Transactional
    public ReissueResponse reissue(ReissueRequest request) {
        validateSignature(request.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(request.refreshToken()))
                .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN));

        if (stored.isExpired(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = stored.getUser();
        String accessToken = createAccessToken(user);
        String rotated = jwtTokenProvider.createRefreshToken(user.getUserId(), user.getEmail(), stored.getDeviceId());

        stored.rotate(hash(rotated), refreshTokenExpiry());

        log.info("토큰 재발급 완료: userId={}, deviceId={}", user.getUserId(), stored.getDeviceId());

        return ReissueResponse.of(accessToken, rotated);
    }

    /**
     * 3-4 로그아웃. 인증된 사용자의 해당 기기 refreshToken 만 폐기한다.
     */
    @Transactional
    public LogoutResponse logout(Long userId, LogoutRequest request) {
        String deviceId = resolveDeviceId(request.deviceId());

        RefreshToken stored = refreshTokenRepository.findByUser_UserIdAndDeviceId(userId, deviceId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN));

        // 다른 기기의 토큰으로 이 기기를 로그아웃시킬 수 없도록 제시된 토큰이 실제 저장된 것인지 확인한다.
        if (!stored.matches(hash(request.refreshToken()))) {
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }

        refreshTokenRepository.delete(stored);

        log.info("로그아웃 완료: userId={}, deviceId={}", userId, deviceId);

        return LogoutResponse.success();
    }

    private String createAccessToken(User user) {
        return jwtTokenProvider.createAccessToken(
                user.getUserId(),
                user.getEmail(),
                List.of(new SimpleGrantedAuthority(ROLE_USER))
        );
    }

    private Instant refreshTokenExpiry() {
        return Instant.now().plus(jwtProperties.refreshTokenValidityInSeconds(), ChronoUnit.SECONDS);
    }

    private String resolveDeviceId(String deviceId) {
        return StringUtils.hasText(deviceId) ? deviceId : DEFAULT_DEVICE_ID;
    }

    /**
     * 위조·만료 토큰이 DB 조회까지 도달하지 않도록 서명을 먼저 검증한다.
     * 실패 사유는 노출하지 않고 INVALID_REFRESH_TOKEN 으로 통합한다.
     */
    private void validateSignature(String refreshToken) {
        try {
            jwtTokenValidator.validateToken(refreshToken);
        } catch (AuthorizationException e) {
            log.warn("Refresh Token 검증 실패: {}", e.getErrorCode().getCode());
            throw new BusinessException(UserErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    /**
     * refresh_token.token_hash 는 원문이 아닌 SHA-256 hex(64자)로 저장한다.
     * 토큰 자체가 128비트 이상 엔트로피를 가진 무작위 값(jti 포함)이므로 salt 없는 단방향 해시로 충분하다.
     */
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
