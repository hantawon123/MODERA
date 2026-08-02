package com.ssafy.modera.api.domain.notification.service;

import com.ssafy.modera.api.domain.notification.dto.PushSendResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "firebase",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DisabledPushMessageSender implements PushMessageSender {
    @Override
    public PushSendResult send(List<String> tokens, Map<String, String> data) {
        return PushSendResult.disabled(tokens.size());
    }
}
