package com.ssafy.modera.api.domain.notification.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class UserDataChangeOutboxCleanupSchedulerTest {

    @Mock UserDataChangeOutboxRepository outboxRepository;

    private UserDataChangeOutboxCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new UserDataChangeOutboxCleanupScheduler(outboxRepository);
        setField(scheduler, "retentionDays", 7);
        setField(scheduler, "batchSize", 1000);
        setField(scheduler, "maxBatches", 10);
    }

    @Test
    void deletesOnlySentEventsOlderThanTheRetentionCutoffInBatches() {
        when(outboxRepository.deleteSentBefore(
                org.mockito.ArgumentMatchers.any(OffsetDateTime.class),
                org.mockito.ArgumentMatchers.eq(1000)))
                .thenReturn(1000, 25);
        OffsetDateTime expectedCutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);

        scheduler.cleanup();

        ArgumentCaptor<OffsetDateTime> cutoff = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(outboxRepository, times(2)).deleteSentBefore(cutoff.capture(),
                org.mockito.ArgumentMatchers.eq(1000));
        assertThat(cutoff.getAllValues()).allSatisfy(value ->
                assertThat(value).isBetween(
                        expectedCutoff.minusSeconds(1), expectedCutoff.plusSeconds(1)));
    }
}
