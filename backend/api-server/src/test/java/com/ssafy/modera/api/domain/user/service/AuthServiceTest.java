package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.dto.request.KakaoLoginRequest;
import com.ssafy.modera.api.domain.user.entity.User;
import com.ssafy.modera.api.domain.user.repository.RefreshTokenRepository;
import com.ssafy.modera.api.domain.user.repository.UserRepository;
import com.ssafy.modera.api.global.security.jwt.JwtProperties;
import com.ssafy.modera.api.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
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

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtTokenProvider,
                jwtProperties,
                kakaoClient
        );
    }

    @Test
    void createsKakaoUserWithConsentedEmail() {
        KakaoClient.KakaoUser kakaoUser = kakaoUser(123L, " User@Example.com ");
        when(kakaoClient.getUser("code")).thenReturn(kakaoUser);
        when(userRepository.findByProviderAndProviderId("KAKAO", "123")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.createAccessToken(any())).thenReturn("access");
        when(jwtTokenProvider.createRefreshToken(any(), any())).thenReturn("refresh");
        when(refreshTokenRepository.findByUserIdAndDeviceId(any(), any())).thenReturn(Optional.empty());
        when(jwtProperties.getRefreshTokenValidityInSeconds()).thenReturn(1209600L);

        authService.kakaoLogin(new KakaoLoginRequest("code", "device"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
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
}
