package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.entity.User;
import com.ssafy.modera.api.domain.user.repository.UserRepository;
import com.ssafy.modera.api.domain.user.repository.UserSettingCommandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
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

    @InjectMocks
    private KakaoUserTransactionService kakaoUserTransactionService;

    @Test
    void createsKakaoUserAndDefaultSettings() {
        when(userRepository.findByProviderAndProviderId("KAKAO", "123"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "userId", 2);
            return saved;
        });

        var result = kakaoUserTransactionService.resolve("123", "user@example.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getProvider()).isEqualTo("KAKAO");
        assertThat(userCaptor.getValue().getProviderId()).isEqualTo("123");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(result.userId()).isEqualTo(2);
        assertThat(result.provider()).isEqualTo("KAKAO");
        verify(userSettingCommandRepository).createDefaults(2);
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

        var result = kakaoUserTransactionService.resolve("123", "user@example.com");

        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(result.userId()).isEqualTo(3);
        verify(userRepository, never()).save(any());
        verify(userSettingCommandRepository, never()).createDefaults(any());
    }
}
