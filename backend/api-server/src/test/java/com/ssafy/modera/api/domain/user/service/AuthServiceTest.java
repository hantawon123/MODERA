package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.dto.request.KakaoLoginRequest;
import com.ssafy.modera.api.domain.user.dto.request.LoginRequest;
import com.ssafy.modera.api.domain.user.dto.request.LogoutRequest;
import com.ssafy.modera.api.domain.user.dto.request.RefreshRequest;
import com.ssafy.modera.api.domain.user.dto.request.RegisterRequest;
import com.ssafy.modera.api.domain.user.entity.RefreshToken;
import com.ssafy.modera.api.domain.user.entity.User;
import com.ssafy.modera.api.domain.user.repository.RefreshTokenRepository;
import com.ssafy.modera.api.domain.user.repository.UserRepository;
import com.ssafy.modera.api.domain.user.repository.UserSettingCommandRepository;
import com.ssafy.modera.api.global.security.jwt.JwtProperties;
import com.ssafy.modera.api.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private KakaoClient kakaoClient;
    @Mock
    private UserSettingCommandRepository userSettingCommandRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                jwtProperties,
                kakaoClient,
                userSettingCommandRepository
        );
    }

    @Test
    void completesLocalRegisterLoginRefreshAndLogoutFlow() throws Exception {
        RegisterRequest registerRequest =
                new RegisterRequest("tester01", "password123", "tester01@example.com");
        when(userRepository.existsByLoginId("tester01")).thenReturn(false);
        when(userRepository.existsByEmail("tester01@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "userId", 1);
            return saved;
        });

        var registered = authService.register(registerRequest);

        assertThat(registered.userId()).isEqualTo(1);
        verify(userSettingCommandRepository).createDefaults(1);

        User user = User.builder()
                .provider("LOCAL")
                .loginId("tester01")
                .passwordHash("bcrypt-hash")
                .email("tester01@example.com")
                .updatedAt(OffsetDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "userId", 1);
        when(userRepository.findByLoginId("tester01")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "bcrypt-hash")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1))
                .thenReturn("access-1", "access-2");
        when(jwtTokenProvider.createRefreshToken(1, "android-device"))
                .thenReturn("refresh-1", "refresh-2");
        when(refreshTokenRepository.findByUserIdAndDeviceId(1, "android-device"))
                .thenReturn(Optional.empty());
        when(jwtProperties.getRefreshTokenValidityInSeconds()).thenReturn(1209600L);

        var loggedIn = authService.login(
                new LoginRequest("tester01", "password123", "android-device")
        );

        assertThat(loggedIn.accessToken()).isEqualTo("access-1");
        assertThat(loggedIn.refreshToken()).isEqualTo("refresh-1");
        ArgumentCaptor<RefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken stored = tokenCaptor.getValue();
        assertThat(stored.getTokenHash()).isEqualTo(sha256("refresh-1"));

        when(jwtTokenProvider.isValid("refresh-1")).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(sha256("refresh-1")))
                .thenReturn(Optional.of(stored));

        var refreshed = authService.refresh(
                new RefreshRequest("refresh-1", "android-device")
        );

        assertThat(refreshed.accessToken()).isEqualTo("access-2");
        assertThat(refreshed.refreshToken()).isEqualTo("refresh-2");
        assertThat(stored.getTokenHash()).isEqualTo(sha256("refresh-2"));

        when(refreshTokenRepository.findByUserIdAndDeviceId(1, "android-device"))
                .thenReturn(Optional.of(stored));

        var loggedOut = authService.logout(
                1,
                new LogoutRequest("refresh-2", "android-device")
        );

        assertThat(loggedOut.loggedOut()).isTrue();
        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void createsKakaoUserWithConsentedEmail() {
        KakaoClient.KakaoUser kakaoUser = kakaoUser(123L, " User@Example.com ");
        when(kakaoClient.getUser("code")).thenReturn(kakaoUser);
        when(userRepository.findByProviderAndProviderId("KAKAO", "123")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "userId", 2);
            return saved;
        });
        when(jwtTokenProvider.createAccessToken(any())).thenReturn("access");
        when(jwtTokenProvider.createRefreshToken(any(), any())).thenReturn("refresh");
        when(refreshTokenRepository.findByUserIdAndDeviceId(any(), any())).thenReturn(Optional.empty());
        when(jwtProperties.getRefreshTokenValidityInSeconds()).thenReturn(1209600L);

        authService.kakaoLogin(new KakaoLoginRequest("code", "device"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        verify(userSettingCommandRepository).createDefaults(2);
    }

    @Test
    void fillsMissingEmailForExistingKakaoUser() {
        User user = User.builder()
                .provider("KAKAO")
                .providerId("123")
                .updatedAt(OffsetDateTime.now())
                .build();
        KakaoClient.KakaoUser kakaoUser = kakaoUser(123L, "user@example.com");
        when(kakaoClient.getUser("code")).thenReturn(kakaoUser);
        when(userRepository.findByProviderAndProviderId("KAKAO", "123")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(jwtTokenProvider.createAccessToken(any())).thenReturn("access");
        when(jwtTokenProvider.createRefreshToken(any(), any())).thenReturn("refresh");
        when(refreshTokenRepository.findByUserIdAndDeviceId(any(), any())).thenReturn(Optional.empty());
        when(jwtProperties.getRefreshTokenValidityInSeconds()).thenReturn(1209600L);

        authService.kakaoLogin(new KakaoLoginRequest("code", "device"));

        assertThat(user.getEmail()).isEqualTo("user@example.com");
    }

    private KakaoClient.KakaoUser kakaoUser(Long id, String email) {
        return new KakaoClient.KakaoUser(id, new KakaoClient.KakaoAccount(email));
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(value.getBytes(StandardCharsets.UTF_8))
        );
    }
}
