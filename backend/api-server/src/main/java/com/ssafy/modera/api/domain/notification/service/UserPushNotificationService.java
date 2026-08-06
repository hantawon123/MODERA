package com.ssafy.modera.api.domain.notification.service;

import com.ssafy.modera.api.domain.notification.dto.PushSendResult;
import com.ssafy.modera.api.domain.notification.outbox.UserDataChangeResource;
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
            UUID eventId,
            Integer userId,
            String resource,
            String resourceId,
            OffsetDateTime occurredAt
    ) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", "DATA_CHANGED");
        data.put("eventId", eventId.toString());
        if (UserDataChangeResource.IMAGE_DELETE_BATCH.equals(resource)) {
            data.put("resource", UserDataChangeResource.IMAGE);
            data.put("changeType", "DELETED");
            data.put("resourceIds", resourceId);
        } else {
            data.put("resource", resource);
            if (resourceId != null && !resourceId.isBlank()) {
                data.put("resourceId", resourceId);
            }
        }
        data.put("occurredAt", occurredAt.toString());
        return pushMessageSender.send(
                pushTokenQueryRepository.findActiveTokens(userId),
                Map.copyOf(data)
        );
    }
}
