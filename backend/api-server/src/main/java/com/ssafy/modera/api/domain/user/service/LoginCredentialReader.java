package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.entity.User;
import com.ssafy.modera.api.domain.user.exception.UserErrorCode;
import com.ssafy.modera.api.domain.user.repository.UserRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginCredentialReader {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public LoginCredential findByLoginId(String loginId) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.LOGIN_FAILED));
        return new LoginCredential(user.getUserId(), user.getProvider(), user.getPasswordHash());
    }

    public record LoginCredential(Integer userId, String provider, String passwordHash) {
    }
}
