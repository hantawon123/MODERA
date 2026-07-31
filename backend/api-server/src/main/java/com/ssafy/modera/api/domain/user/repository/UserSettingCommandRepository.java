package com.ssafy.modera.api.domain.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserSettingCommandRepository {

    private final JdbcTemplate jdbcTemplate;

    public void createDefaults(Integer userId) {
        jdbcTemplate.update(
                """
                INSERT INTO user_schema.user_setting (
                    user_id,
                    server_analysis_enabled,
                    notification_enabled,
                    del_yn
                )
                VALUES (?, TRUE, FALSE, 'N')
                """,
                userId
        );
    }
}
