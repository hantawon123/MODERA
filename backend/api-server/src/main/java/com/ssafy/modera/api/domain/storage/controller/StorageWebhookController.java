package com.ssafy.modera.api.domain.storage.controller;

import com.ssafy.modera.api.domain.storage.dto.MinioWebhookEvent;
import com.ssafy.modera.api.domain.storage.service.StorageWebhookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StorageWebhookController {

    private final StorageWebhookService storageWebhookService;
    private final String webhookToken;

    public StorageWebhookController(StorageWebhookService storageWebhookService,
                                     @Value("${internal.storage.webhook-token}") String webhookToken) {
        this.storageWebhookService = storageWebhookService;
        this.webhookToken = webhookToken;
    }

    @PostMapping("/internal/storage/events")
    public ResponseEntity<Void> handleStorageEvent(
            @RequestHeader(value = "X-Webhook-Token", required = false) String webhookHeader,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody MinioWebhookEvent event
    ) {
        boolean authenticated = webhookToken.equals(webhookHeader)
                || ("Bearer " + webhookToken).equals(authorization);
        if (!authenticated) {
            return ResponseEntity.status(401).build();
        }

        storageWebhookService.handle(event);
        return ResponseEntity.ok().build();
    }
}
