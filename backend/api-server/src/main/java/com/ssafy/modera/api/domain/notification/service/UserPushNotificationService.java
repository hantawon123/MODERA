package com.ssafy.modera.api.domain.notification.service;

import com.ssafy.modera.api.domain.notification.dto.PushSendResult;
import com.ssafy.modera.api.domain.user.repository.UserPushTokenQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPushNotificationService {

    private final UserPushTokenQueryRepository pushTokenQueryRepository;
    private final PushMessageSender pushMessageSender;

    public PushSendResult sendDataChanged(
            Integer userId,
            String resource,
            String resourceId
    ) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", "DATA_CHANGED");
        data.put("eventId", UUID.randomUUID().toString());
        data.put("resource", resource);
        if (resourceId != null && !resourceId.isBlank()) {
            data.put("resourceId", resourceId);
        }
        data.put("occurredAt", OffsetDateTime.now().toString());
        return pushMessageSender.send(
                pushTokenQueryRepository.findActiveTokens(userId),
                Map.copyOf(data)
        );
    }
}
