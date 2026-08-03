package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.entity.User;
import com.ssafy.modera.api.domain.user.exception.UserErrorCode;
import com.ssafy.modera.api.domain.user.repository.UserRepository;
import com.ssafy.modera.api.domain.user.repository.UserSettingCommandRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
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

    @Transactional
    public AuthenticatedKakaoUser resolve(String providerId, String email) {
        User user = userRepository.findByProviderAndProviderId(PROVIDER_KAKAO, providerId)
                .orElseGet(() -> createUser(providerId, email));
        synchronizeEmail(user, email);
        return new AuthenticatedKakaoUser(user.getUserId(), user.getProvider());
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

    public record AuthenticatedKakaoUser(Integer userId, String provider) {
    }
}
