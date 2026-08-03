package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.entity.User;
import com.ssafy.modera.api.domain.user.exception.UserErrorCode;
import com.ssafy.modera.api.domain.user.repository.UserRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginCredentialReaderTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoginCredentialReader loginCredentialReader;

    @Test
    void returnsOnlyFieldsRequiredForPasswordVerification() {
        User user = User.builder()
                .provider("LOCAL")
                .loginId("tester01")
                .passwordHash("bcrypt-hash")
                .email("tester@example.com")
                .updatedAt(OffsetDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "userId", 1);
        when(userRepository.findByLoginId("tester01")).thenReturn(Optional.of(user));

        var credential = loginCredentialReader.findByLoginId("tester01");

        assertThat(credential.userId()).isEqualTo(1);
        assertThat(credential.provider()).isEqualTo("LOCAL");
        assertThat(credential.passwordHash()).isEqualTo("bcrypt-hash");
    }

    @Test
    void hidesWhetherLoginIdExists() {
        when(userRepository.findByLoginId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginCredentialReader.findByLoginId("missing"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(UserErrorCode.LOGIN_FAILED);
    }
}
