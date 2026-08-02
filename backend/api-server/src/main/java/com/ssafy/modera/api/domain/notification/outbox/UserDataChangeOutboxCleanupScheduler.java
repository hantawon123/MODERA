package com.ssafy.modera.api.domain.notification.outbox;

import com.ssafy.modera.api.global.cleanup.SoftDeletedDataCleanupRepository;
import com.ssafy.modera.api.global.cleanup.SoftDeletedDataCleanupResult;
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
    private final SoftDeletedDataCleanupRepository softDeletedDataCleanupRepository;

    @Value("${notification.outbox.cleanup.retention-days:7}")
    private int retentionDays;

    @Value("${notification.outbox.cleanup.batch-size:1000}")
    private int batchSize;

    @Value("${notification.outbox.cleanup.max-batches:10}")
    private int maxBatches;

    @Value("${notification.outbox.cleanup.soft-delete-batch-size:1000}")
    private int softDeleteBatchSize;

    @Value("${notification.outbox.cleanup.soft-delete-max-batches:10}")
    private int softDeleteMaxBatches;

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

        cleanupSoftDeletedData();
    }

    private void cleanupSoftDeletedData() {
        int totalDeleted = 0;
        for (int batch = 0; batch < softDeleteMaxBatches; batch++) {
            SoftDeletedDataCleanupResult result =
                    softDeletedDataCleanupRepository.deleteBatch(softDeleteBatchSize);
            totalDeleted += result.deletedCount();
            if (!result.mayHaveMore()) {
                break;
            }
        }
        if (totalDeleted > 0) {
            log.info("Deleted soft-deleted library/query rows: count={}", totalDeleted);
        }
    }
}
