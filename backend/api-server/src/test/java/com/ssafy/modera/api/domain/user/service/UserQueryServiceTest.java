package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.repository.UserInfoRow;
import com.ssafy.modera.api.domain.user.repository.UserQueryRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock UserQueryRepository userQueryRepository;
    @InjectMocks UserQueryService userQueryService;

    @Test
    void returnsAccountAndSettings() {
        when(userQueryRepository.findUserInfo(1))
                .thenReturn(Optional.of(new UserInfoRow(
                        1, "newUser123", "user@example.com", true, false)));

        var response = userQueryService.getMyInfo(1);

        assertThat(response.userId()).isEqualTo(1);
        assertThat(response.loginId()).isEqualTo("newUser123");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.notification()).isTrue();
        assertThat(response.backgroundAnalysis()).isFalse();
    }

    @Test
    void rejectsMissingUser() {
        when(userQueryRepository.findUserInfo(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQueryService.getMyInfo(999))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception).getErrorCode().getCode())
                        .isEqualTo("USER_NOT_FOUND"));
    }
}
