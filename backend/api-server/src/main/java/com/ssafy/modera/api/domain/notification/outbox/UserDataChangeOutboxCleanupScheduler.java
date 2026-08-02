package com.ssafy.modera.api.domain.notification.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataChangeOutboxCleanupScheduler {

    private final UserDataChangeOutboxRepository outboxRepository;

    @Value("${notification.outbox.cleanup.retention-days:7}")
    private int retentionDays;

    @Value("${notification.outbox.cleanup.batch-size:1000}")
    private int batchSize;

    @Value("${notification.outbox.cleanup.max-batches:10}")
    private int maxBatches;

    @Scheduled(
            cron = "${notification.outbox.cleanup.cron:0 0 4 * * *}",
            zone = "${notification.outbox.cleanup.zone:Asia/Seoul}"
    )
    public void cleanup() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(retentionDays);
        int totalDeleted = 0;

        for (int batch = 0; batch < maxBatches; batch++) {
            int deleted = outboxRepository.deleteSentBefore(cutoff, batchSize);
            totalDeleted += deleted;
            if (deleted < batchSize) {
                break;
            }
        }

        if (totalDeleted > 0) {
            log.info("Deleted old SENT outbox events: count={}, cutoff={}",
                    totalDeleted, cutoff);
        }
    }
}
