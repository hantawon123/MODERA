package com.ssafy.modera.api.domain.image.controller;

import com.ssafy.modera.api.domain.image.dto.ImageDetailResponse;
import com.ssafy.modera.api.domain.image.dto.ImageRegisterRequest;
import com.ssafy.modera.api.domain.image.dto.ImageRegisterResponse;
import com.ssafy.modera.api.domain.image.service.ImageQueryService;
import com.ssafy.modera.api.domain.image.service.ImageRegistrationService;
import com.ssafy.modera.api.global.response.ApiResponse;
import com.ssafy.modera.api.global.response.ApiV1Controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@ApiV1Controller
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageRegistrationService imageRegistrationService;
    private final ImageQueryService imageQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ImageRegisterResponse>> register(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ImageRegisterRequest request
    ) {
        ImageRegisterResponse response = imageRegistrationService.register(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<ApiResponse<ImageDetailResponse>> getImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable UUID imageId
    ) {
        ImageDetailResponse response = imageQueryService.getImage(userId, imageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
