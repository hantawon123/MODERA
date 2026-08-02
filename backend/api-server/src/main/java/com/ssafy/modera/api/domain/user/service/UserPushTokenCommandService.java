package com.ssafy.modera.api.domain.user.service;

import com.ssafy.modera.api.domain.user.dto.request.PushTokenRegisterRequest;
import com.ssafy.modera.api.domain.user.dto.response.PushTokenRegistrationResponse;
import com.ssafy.modera.api.domain.user.repository.UserPushTokenCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPushTokenCommandService {

    private final UserPushTokenCommandRepository userPushTokenCommandRepository;

    @Transactional
    public PushTokenRegistrationResponse register(
            Integer userId,
            PushTokenRegisterRequest request
    ) {
        String deviceId = request.deviceId().trim();
        userPushTokenCommandRepository.upsert(
                userId,
                deviceId,
                request.fcmToken().trim()
        );
        return new PushTokenRegistrationResponse(deviceId);
    }

    @Transactional
    public void delete(Integer userId, String deviceId) {
        userPushTokenCommandRepository.delete(userId, deviceId.trim());
    }
}
