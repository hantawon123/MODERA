package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.entity.User;
import com.ssafy.modera.api.domain.user.repository.UserRepository;
import com.ssafy.modera.api.domain.user.repository.UserSettingCommandRepository;
import com.ssafy.modera.api.global.security.jwt.JwtProperties;
import com.ssafy.modera.api.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KakaoUserTransactionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSettingCommandRepository userSettingCommandRepository;
    @Mock
    private RefreshTokenCommandService refreshTokenCommandService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private KakaoUserTransactionService kakaoUserTransactionService;

    @Test
    void createsKakaoUserAndDefaultSettings() throws Exception {
        when(userRepository.findByProviderAndProviderId("KAKAO", "123"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "userId", 2);
            return saved;
        });
        prepareTokens(2, "device");

        var result = kakaoUserTransactionService.login("123", "user@example.com", "device");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getProvider()).isEqualTo("KAKAO");
        assertThat(userCaptor.getValue().getProviderId()).isEqualTo("123");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(result.userId()).isEqualTo(2);
        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        verify(userSettingCommandRepository).createDefaults(2);
        verify(refreshTokenCommandService).upsert(
                org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.eq("device"),
                org.mockito.ArgumentMatchers.eq(sha256("refresh")),
                any(OffsetDateTime.class)
        );
    }

    @Test
    void synchronizesChangedEmailForExistingKakaoUser() {
        User user = User.builder()
                .provider("KAKAO")
                .providerId("123")
                .updatedAt(OffsetDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "userId", 3);
        when(userRepository.findByProviderAndProviderId("KAKAO", "123"))
                .thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        prepareTokens(3, "device");

        var result = kakaoUserTransactionService.login("123", "user@example.com", "device");

        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(result.userId()).isEqualTo(3);
        verify(userRepository, never()).save(any());
        verify(userSettingCommandRepository, never()).createDefaults(any());
    }

    @Test
    void keepsExistingKakaoUserWhenEmailIsUnchanged() {
        User user = User.builder()
                .provider("KAKAO")
                .providerId("123")
                .email("user@example.com")
                .updatedAt(OffsetDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "userId", 4);
        when(userRepository.findByProviderAndProviderId("KAKAO", "123"))
                .thenReturn(Optional.of(user));
        prepareTokens(4, "device");

        var result = kakaoUserTransactionService.login("123", "user@example.com", "device");

        assertThat(result.userId()).isEqualTo(4);
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
        verify(refreshTokenCommandService).upsert(
                org.mockito.ArgumentMatchers.eq(4),
                org.mockito.ArgumentMatchers.eq("device"),
                org.mockito.ArgumentMatchers.eq(RefreshTokenHash.sha256("refresh")),
                any(OffsetDateTime.class)
        );
    }

    private void prepareTokens(Integer userId, String deviceId) {
        when(jwtTokenProvider.createAccessToken(userId)).thenReturn("access");
        when(jwtTokenProvider.createRefreshToken(userId, deviceId)).thenReturn("refresh");
        when(jwtProperties.getRefreshTokenValidityInSeconds()).thenReturn(1209600L);
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256")
                        .digest(value.getBytes(StandardCharsets.UTF_8))
        );
    }
}
