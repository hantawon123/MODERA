package com.ssafy.modera.api.domain.notification.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserDataChangeOutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public UUID insert(Integer userId, String resource, String resourceId) {
        UUID outboxId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO event_schema.user_data_change_outbox (
                    outbox_id, user_id, resource, resource_id
                ) VALUES (?, ?, ?, ?)
                """,
                outboxId, userId, resource, resourceId
        );
        return outboxId;
    }

    public List<UserDataChangeOutboxEvent> claim(int batchSize, int staleSeconds) {
        return jdbcTemplate.query(
                """
                WITH candidates AS (
                    SELECT outbox_id
                    FROM event_schema.user_data_change_outbox
                    WHERE (
                            status = 'PENDING'
                            AND available_at <= now()
                        ) OR (
                            status = 'PROCESSING'
                            AND processing_started_at < now() - make_interval(secs => ?)
                        )
                    ORDER BY available_at, created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                )
                UPDATE event_schema.user_data_change_outbox outbox
                SET status = 'PROCESSING', processing_started_at = now()
                FROM candidates
                WHERE outbox.outbox_id = candidates.outbox_id
                RETURNING outbox.outbox_id, outbox.user_id, outbox.resource,
                          outbox.resource_id, outbox.retry_count, outbox.created_at
                """,
                (rs, rowNum) -> new UserDataChangeOutboxEvent(
                        rs.getObject("outbox_id", UUID.class),
                        rs.getInt("user_id"),
                        rs.getString("resource"),
                        rs.getString("resource_id"),
                        rs.getInt("retry_count"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                staleSeconds,
                batchSize
        );
    }

    public void markSent(UUID outboxId) {
        jdbcTemplate.update(
                """
                UPDATE event_schema.user_data_change_outbox
                SET status = 'SENT', sent_at = now(), processing_started_at = NULL
                WHERE outbox_id = ? AND status = 'PROCESSING'
                """,
                outboxId
        );
    }

    public void markRetry(UUID outboxId, int retryCount, OffsetDateTime availableAt) {
        jdbcTemplate.update(
                """
                UPDATE event_schema.user_data_change_outbox
                SET status = 'PENDING', retry_count = ?, available_at = ?,
                    processing_started_at = NULL
                WHERE outbox_id = ? AND status = 'PROCESSING'
                """,
                retryCount,
                availableAt,
                outboxId
        );
    }

    public void markFailed(UUID outboxId, int retryCount) {
        jdbcTemplate.update(
                """
                UPDATE event_schema.user_data_change_outbox
                SET status = 'FAILED', retry_count = ?, processing_started_at = NULL
                WHERE outbox_id = ? AND status = 'PROCESSING'
                """,
                retryCount,
                outboxId
        );
    }
}
