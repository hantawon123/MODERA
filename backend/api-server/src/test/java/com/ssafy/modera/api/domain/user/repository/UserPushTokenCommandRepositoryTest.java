package com.ssafy.modera.api.domain.user.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserPushTokenCommandRepositoryTest {

    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks UserPushTokenCommandRepository repository;

    @Test
    void deactivatesTokenFromAnotherDeviceBeforeUpsert() {
        repository.upsert(7, "device-1", "fcm-token");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(2)).update(sql.capture(), any(Object[].class));
        assertThat(sql.getAllValues().get(0))
                .contains("fcm_token = ?")
                .contains("del_yn = 'Y'");
        assertThat(sql.getAllValues().get(1))
                .contains("ON CONFLICT (user_id, device_id)")
                .contains("fcm_token = EXCLUDED.fcm_token");
    }
}
