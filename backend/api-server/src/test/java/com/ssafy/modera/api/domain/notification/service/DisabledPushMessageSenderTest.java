package com.ssafy.modera.api.domain.notification.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DisabledPushMessageSenderTest {

    @Test
    void reportsDisabledWithoutPretendingMessagesWereSent() {
        var result = new DisabledPushMessageSender()
                .send(List.of("token-a"), Map.of("type", "DATA_CHANGED"));

        assertThat(result.enabled()).isFalse();
        assertThat(result.targetCount()).isEqualTo(1);
        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isZero();
    }
}
