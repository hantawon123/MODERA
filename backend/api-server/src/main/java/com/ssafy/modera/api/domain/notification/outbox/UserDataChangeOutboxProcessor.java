package com.ssafy.modera.api.domain.notification.outbox;

import com.ssafy.modera.api.domain.notification.dto.PushSendResult;
import com.ssafy.modera.api.domain.notification.service.UserPushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class UserDataChangeOutboxProcessor {

    private final UserDataChangeOutboxRepository outboxRepository;
    private final UserPushNotificationService pushNotificationService;

    @Value("${notification.outbox.batch-size:100}")
    private int batchSize;

    @Value("${notification.outbox.max-retries:5}")
    private int maxRetries;

    @Value("${notification.outbox.stale-seconds:300}")
    private int staleSeconds;

    @Scheduled(fixedDelayString = "${notification.outbox.poll-delay-ms:1000}")
    public void process() {
        for (UserDataChangeOutboxEvent event :
                outboxRepository.claim(batchSize, staleSeconds)) {
            dispatch(event);
        }
    }

    private void dispatch(UserDataChangeOutboxEvent event) {
        try {
            PushSendResult result = pushNotificationService.sendDataChanged(
                    event.outboxId(),
                    event.userId(),
                    event.resource(),
                    event.resourceId(),
                    event.createdAt()
            );
            if (!result.enabled() || result.failureCount() > 0) {
                retry(event);
                return;
            }
            outboxRepository.markSent(event.outboxId());
        } catch (RuntimeException exception) {
            log.warn("FCM data-change dispatch failed: outboxId={}, retryCount={}",
                    event.outboxId(), event.retryCount(), exception);
            retry(event);
        }
    }

    private void retry(UserDataChangeOutboxEvent event) {
        int nextRetryCount = event.retryCount() + 1;
        if (nextRetryCount >= maxRetries) {
            outboxRepository.markFailed(event.outboxId(), nextRetryCount);
            return;
        }
        long delaySeconds = Math.min(300L, 1L << Math.min(nextRetryCount, 8));
        outboxRepository.markRetry(
                event.outboxId(),
                nextRetryCount,
                OffsetDateTime.now().plusSeconds(delaySeconds)
        );
    }
}
