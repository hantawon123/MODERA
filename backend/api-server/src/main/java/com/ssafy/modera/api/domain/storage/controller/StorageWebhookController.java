package com.ssafy.modera.api.domain.storage.controller;

import com.ssafy.modera.api.domain.storage.dto.MinioWebhookEvent;
import com.ssafy.modera.api.domain.storage.service.StorageWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class StorageWebhookController {

    private static final String BEARER_PREFIX = "Bearer ";
    private final StorageWebhookService storageWebhookService;
    private final String webhookToken;

    public StorageWebhookController(StorageWebhookService storageWebhookService,
                                     @Value("${internal.storage.webhook-token}") String webhookToken) {
        this.storageWebhookService = storageWebhookService;
        this.webhookToken = webhookToken;
    }

    /**
     * MinIO의 ObjectCreated 알림을 받는다.
     * MINIO는 notify_webhook의 auth_token 값을 Authorization 헤더로 보내며 헤더명을 바꿀 수 없다.
     * X-Webhook-Token은 로컬/수동 테스트 호환을 위해 함께 허용한다.
     */
    @PostMapping("/internal/storage/events")
    public ResponseEntity<Void> handleStorageEvent(
            @RequestHeader(value = "X-Webhook-Token", required = false) String webhookHeader,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody MinioWebhookEvent event
    ) {
        log.info("webhook 헤더 확인: x-webhook={} authorization={}", webhookHeader != null, authorizationHeader != null);

        if (!isAuthorized(webhookHeader, authorizationHeader)) {
            return ResponseEntity.status(401).build();
        }

        storageWebhookService.handle(event);
        return ResponseEntity.ok().build();
    }

    private boolean isAuthorized(String webhookHeader, String authorizationHeader) {
        String presented = webhookHeader != null ? webhookHeader : stripBearer(authorizationHeader);
        return presented != null && webhookToken.equals(presented);
    }
    private String stripBearer(String value) {
        if (value == null) {
            return null;
        }
        return value.startsWith(BEARER_PREFIX) ? value.substring(BEARER_PREFIX.length()) : value;
    }
}
