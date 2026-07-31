package com.ssafy.modera.api.domain.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<UserInfoRow> findUserInfo(Integer userId) {
        return jdbcTemplate.query(
                """
                SELECT
                    u.user_id,
                    u.login_id,
                    u.email,
                    COALESCE(s.notification_enabled, FALSE) AS notification,
                    COALESCE(s.server_analysis_enabled, TRUE) AS background_analysis
                FROM user_schema.users u
                LEFT JOIN user_schema.user_setting s
                  ON s.user_id = u.user_id
                 AND s.del_yn = 'N'
                WHERE u.user_id = ?
                  AND u.del_yn = 'N'
                """,
                (rs, rowNum) -> new UserInfoRow(
                        rs.getInt("user_id"),
                        rs.getString("login_id"),
                        rs.getString("email"),
                        rs.getBoolean("notification"),
                        rs.getBoolean("background_analysis")
                ),
                userId
        ).stream().findFirst();
    }
}
