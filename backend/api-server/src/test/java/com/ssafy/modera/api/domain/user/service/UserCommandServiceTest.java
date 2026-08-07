package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.repository.UserDataResetRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock UserDataResetRepository userDataResetRepository;
    @InjectMocks UserCommandService userCommandService;

    @Test
    void softDeletesStoredDataButKeepsTheAccount() {
        when(userDataResetRepository.existsActiveUser(2)).thenReturn(true);

        var response = userCommandService.deleteStoredData(
                2);

        assertThat(response).isNotNull();
        verify(userDataResetRepository).softDeleteAll(2);
    }

    @Test
    void rejectsMissingUser() {
        when(userDataResetRepository.existsActiveUser(999)).thenReturn(false);

        assertThatThrownBy(() -> userCommandService.deleteStoredData(
                999))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(
                        ((BusinessException) exception).getErrorCode().getCode())
                        .isEqualTo("USER_NOT_FOUND"));

        verify(userDataResetRepository, never()).softDeleteAll(999);
    }
}
