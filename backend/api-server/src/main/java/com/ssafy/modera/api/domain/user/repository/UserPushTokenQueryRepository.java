package com.ssafy.modera.api.domain.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserPushTokenQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<String> findActiveTokens(Integer userId) {
        return jdbcTemplate.queryForList(
                """
                SELECT fcm_token
                FROM user_schema.user_push_token
                WHERE user_id = ? AND del_yn = 'N'
                ORDER BY push_token_id
                """,
                String.class,
                userId
        );
    }
}
