package com.ssafy.modera.api.domain.notification.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.ssafy.modera.api.domain.notification.dto.PushSendResult;
import com.ssafy.modera.api.domain.user.repository.UserPushTokenCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class FirebasePushMessageSender implements PushMessageSender {

    private static final int MULTICAST_LIMIT = 500;

    private final FirebaseMessaging firebaseMessaging;
    private final UserPushTokenCommandRepository pushTokenCommandRepository;

    @Override
    public PushSendResult send(List<String> tokens, Map<String, String> data) {
        int success = 0;
        int failure = 0;
        for (int from = 0; from < tokens.size(); from += MULTICAST_LIMIT) {
            List<String> chunk = tokens.subList(
                    from,
                    Math.min(from + MULTICAST_LIMIT, tokens.size())
            );
            try {
                BatchResponse response = firebaseMessaging.sendEachForMulticast(
                        MulticastMessage.builder()
                                .addAllTokens(chunk)
                                .putAllData(data)
                                .setAndroidConfig(AndroidConfig.builder()
                                        .setPriority(AndroidConfig.Priority.HIGH)
                                        .build())
                                .build()
                );
                success += response.getSuccessCount();
                failure += response.getFailureCount();
                removeInvalidTokens(chunk, response.getResponses());
            } catch (FirebaseMessagingException exception) {
                failure += chunk.size();
                log.warn("FCM multicast 전송 실패: targetCount={}, errorCode={}",
                        chunk.size(), exception.getMessagingErrorCode());
            }
        }
        return new PushSendResult(true, tokens.size(), success, failure);
    }

    private void removeInvalidTokens(List<String> tokens, List<SendResponse> responses) {
        for (int index = 0; index < responses.size(); index++) {
            SendResponse response = responses.get(index);
            if (response.isSuccessful() || response.getException() == null) {
                continue;
            }
            MessagingErrorCode errorCode = response.getException().getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED
                    || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                pushTokenCommandRepository.deleteByToken(tokens.get(index));
            }
        }
    }
}
