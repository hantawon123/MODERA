package com.ssafy.modera.api.domain.notification.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDataChangeOutboxService {

    private final UserDataChangeOutboxRepository outboxRepository;

    public UUID record(Integer userId, String resource, String resourceId) {
        return outboxRepository.insert(userId, resource, resourceId);
    }
}
