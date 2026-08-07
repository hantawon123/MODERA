package com.ssafy.modera.api.domain.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
@RequiredArgsConstructor
public class RefreshTokenCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    public void upsert(Integer userId, String deviceId, String tokenHash, OffsetDateTime expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO user_schema.refresh_token (
                    user_id, device_id, token_hash, expires_at, created_at
                )
                VALUES (?, ?, ?, ?, now())
                ON CONFLICT (user_id, device_id)
                DO UPDATE SET
                    token_hash = EXCLUDED.token_hash,
                    expires_at = EXCLUDED.expires_at
                """, userId, deviceId, tokenHash, expiresAt);
    }
}
