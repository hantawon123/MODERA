package com.ssafy.modera.api.domain.user.controller;

import com.ssafy.modera.api.domain.user.dto.request.PushTokenRegisterRequest;
import com.ssafy.modera.api.domain.user.dto.response.PushTokenRegistrationResponse;
import com.ssafy.modera.api.domain.user.service.UserPushTokenCommandService;
import com.ssafy.modera.api.global.response.ApiResponse;
import com.ssafy.modera.api.global.response.ApiV1Controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 푸시 토큰", description = "사용자 기기의 FCM 토큰 관리")
@ApiV1Controller
@RestController
@RequestMapping("/api/v1/user/devices")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Bearer Authentication")
public class UserPushTokenController {

    private final UserPushTokenCommandService userPushTokenCommandService;

    @Operation(summary = "FCM 토큰 등록 또는 갱신")
    @PutMapping("/push-token")
    public ResponseEntity<ApiResponse<PushTokenRegistrationResponse>> register(
            @AuthenticationPrincipal Integer userId,
            @Valid @RequestBody PushTokenRegisterRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "U203",
                userPushTokenCommandService.register(userId, request)
        ));
    }

    @Operation(summary = "기기의 FCM 토큰 삭제")
    @DeleteMapping("/{deviceId}/push-token")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Integer userId,
            @PathVariable(name = "deviceId")
            @NotBlank @Size(max = 64) String deviceId
    ) {
        userPushTokenCommandService.delete(userId, deviceId);
        return ResponseEntity.ok(ApiResponse.success("U204", null));
    }
}
