package com.ssafy.modera.api.domain.user.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCommandRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private RefreshTokenCommandRepository refreshTokenCommandRepository;

    @Test
    void upsertsOneTokenPerUserAndDeviceInASingleStatement() {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(14);

        refreshTokenCommandRepository.upsert(1, "device-1", "token-hash", expiresAt);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(
                sqlCaptor.capture(), any(), any(), any(), any());
        assertThat(sqlCaptor.getValue())
                .contains("ON CONFLICT (user_id, device_id)")
                .contains("token_hash = EXCLUDED.token_hash")
                .contains("expires_at = EXCLUDED.expires_at");
    }
}
