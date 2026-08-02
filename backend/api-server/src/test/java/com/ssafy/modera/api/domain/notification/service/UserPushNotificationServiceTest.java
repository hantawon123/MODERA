package com.ssafy.modera.api.domain.notification.service;

import com.ssafy.modera.api.domain.notification.dto.PushSendResult;
import com.ssafy.modera.api.domain.user.repository.UserPushTokenQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPushNotificationServiceTest {

    @Mock UserPushTokenQueryRepository tokenQueryRepository;
    @Mock PushMessageSender pushMessageSender;
    @InjectMocks UserPushNotificationService service;

    @Test
    void sendsDataOnlyInvalidationToAllActiveUserTokens() {
        when(tokenQueryRepository.findActiveTokens(7))
                .thenReturn(List.of("token-a", "token-b"));
        when(pushMessageSender.send(org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn(new PushSendResult(true, 2, 2, 0));

        PushSendResult result = service.sendDataChanged(7, "IMAGE_CATEGORY", "41");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> data = ArgumentCaptor.forClass(Map.class);
        verify(pushMessageSender).send(
                org.mockito.ArgumentMatchers.eq(List.of("token-a", "token-b")),
                data.capture()
        );
        assertThat(data.getValue())
                .containsEntry("type", "DATA_CHANGED")
                .containsEntry("resource", "IMAGE_CATEGORY")
                .containsEntry("resourceId", "41")
                .containsKeys("eventId", "occurredAt");
        assertThat(result.successCount()).isEqualTo(2);
    }
}
