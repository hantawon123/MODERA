package com.ssafy.modera.api.domain.notification.controller;

import com.ssafy.modera.api.domain.notification.dto.PushSendResult;
import com.ssafy.modera.api.domain.notification.service.UserPushNotificationService;
import com.ssafy.modera.api.global.response.ApiResponse;
import com.ssafy.modera.api.global.response.ApiV1Controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "푸시 메시지", description = "FCM data-only 메시지 검증")
@ApiV1Controller
@RestController
@RequestMapping("/api/v1/user/devices")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class PushTestController {

    private final UserPushNotificationService userPushNotificationService;

    @Operation(summary = "본인 기기로 FCM 테스트 메시지 발송")
    @PostMapping("/push-test")
    public ResponseEntity<ApiResponse<PushSendResult>> sendTest(
            @AuthenticationPrincipal Integer userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "U205",
                userPushNotificationService.sendDataChanged(userId, "SYNC_TEST", null)
        ));
    }
}
