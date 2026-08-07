package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.dto.request.PushTokenRegisterRequest;
import com.ssafy.modera.api.domain.user.repository.UserPushTokenCommandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserPushTokenCommandServiceTest {

    @Mock UserPushTokenCommandRepository repository;
    @InjectMocks UserPushTokenCommandService service;

    @Test
    void registersTrimmedDeviceAndFcmTokenWithoutReturningTheToken() {
        var response = service.register(
                7,
                new PushTokenRegisterRequest(" device-1 ", " fcm-token ")
        );

        verify(repository).upsert(7, "device-1", "fcm-token");
        assertThat(response.deviceId()).isEqualTo("device-1");
    }

    @Test
    void deletesOnlyTheAuthenticatedUsersDeviceToken() {
        service.delete(7, " device-1 ");

        verify(repository).delete(7, "device-1");
    }
}
