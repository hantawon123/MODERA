package com.ssafy.modera.api.domain.notification.outbox;

import com.ssafy.modera.api.domain.notification.dto.PushSendResult;
import com.ssafy.modera.api.domain.notification.service.UserPushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class UserDataChangeOutboxProcessorTest {

    @Mock UserDataChangeOutboxRepository outboxRepository;
    @Mock UserPushNotificationService pushNotificationService;

    private UserDataChangeOutboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new UserDataChangeOutboxProcessor(
                outboxRepository, pushNotificationService);
        setField(processor, "batchSize", 100);
        setField(processor, "maxRetries", 5);
        setField(processor, "staleSeconds", 300);
    }

    @Test
    void marksTheClaimedEventAsSentAfterSuccessfulDispatch() {
        UserDataChangeOutboxEvent event = event(0);
        when(outboxRepository.claim(100, 300)).thenReturn(List.of(event));
        when(pushNotificationService.sendDataChanged(
                event.outboxId(), 7, "IMAGE_CATEGORY", "18", event.createdAt()))
                .thenReturn(new PushSendResult(true, 1, 1, 0));

        processor.process();

        verify(outboxRepository).markSent(event.outboxId());
        verify(outboxRepository, never()).markRetry(any(), eq(1), any());
    }

    @Test
    void schedulesRetryWhenAnyTargetDispatchFails() {
        UserDataChangeOutboxEvent event = event(0);
        when(outboxRepository.claim(100, 300)).thenReturn(List.of(event));
        when(pushNotificationService.sendDataChanged(
                event.outboxId(), 7, "IMAGE_CATEGORY", "18", event.createdAt()))
                .thenReturn(new PushSendResult(true, 2, 1, 1));

        processor.process();

        verify(outboxRepository).markRetry(eq(event.outboxId()), eq(1), any());
        verify(outboxRepository, never()).markSent(event.outboxId());
    }

    @Test
    void marksTheEventFailedWhenTheRetryLimitIsReached() {
        UserDataChangeOutboxEvent event = event(4);
        when(outboxRepository.claim(100, 300)).thenReturn(List.of(event));
        when(pushNotificationService.sendDataChanged(
                event.outboxId(), 7, "IMAGE_CATEGORY", "18", event.createdAt()))
                .thenThrow(new IllegalStateException("firebase unavailable"));

        processor.process();

        verify(outboxRepository).markFailed(event.outboxId(), 5);
        verify(outboxRepository, never()).markRetry(any(), eq(5), any());
    }

    private UserDataChangeOutboxEvent event(int retryCount) {
        return new UserDataChangeOutboxEvent(
                UUID.randomUUID(), 7, "IMAGE_CATEGORY", "18",
                retryCount, OffsetDateTime.now());
    }
}
