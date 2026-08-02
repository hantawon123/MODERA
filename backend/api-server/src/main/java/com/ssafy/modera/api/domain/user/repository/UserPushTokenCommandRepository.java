package com.ssafy.modera.api.domain.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPushTokenCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    public void upsert(Integer userId, String deviceId, String fcmToken) {
        jdbcTemplate.update(
                """
                UPDATE user_schema.user_push_token
                SET del_yn = 'Y', updated_at = now()
                WHERE fcm_token = ?
                  AND del_yn = 'N'
                  AND (user_id <> ? OR device_id <> ?)
                """,
                fcmToken, userId, deviceId
        );

        jdbcTemplate.update(
                """
                INSERT INTO user_schema.user_push_token (
                    user_id, device_id, fcm_token, del_yn
                ) VALUES (?, ?, ?, 'N')
                ON CONFLICT (user_id, device_id) DO UPDATE SET
                    fcm_token = EXCLUDED.fcm_token,
                    del_yn = 'N',
                    updated_at = now()
                """,
                userId, deviceId, fcmToken
        );
    }

    public void delete(Integer userId, String deviceId) {
        jdbcTemplate.update(
                """
                DELETE FROM user_schema.user_push_token
                WHERE user_id = ? AND device_id = ?
                """,
                userId, deviceId
        );
    }

    public void deleteByToken(String fcmToken) {
        jdbcTemplate.update(
                """
                DELETE FROM user_schema.user_push_token
                WHERE fcm_token = ?
                """,
                fcmToken
        );
    }
}
