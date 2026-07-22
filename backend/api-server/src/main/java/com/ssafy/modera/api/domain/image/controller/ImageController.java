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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    // TODO: JWT 인증 도입 후 SecurityContext에서 userId를 추출하도록 교체하고
    // 이 헤더 기반 임시 인증은 제거한다. SecurityConfig의 permitAll도 함께 걷어낼 것.
    @PostMapping
    public ResponseEntity<ApiResponse<ImageRegisterResponse>> register(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid ImageRegisterRequest request
    ) {
        ImageRegisterResponse response = imageRegistrationService.register(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 2단계(전역 예외 처리)의 404 시나리오를 실제로 보여줄 조회 API가 없어서 최소로 추가했다.
    @GetMapping("/{imageId}")
    public ResponseEntity<ApiResponse<ImageDetailResponse>> getImage(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable UUID imageId
    ) {
        ImageDetailResponse response = imageQueryService.getImage(userId, imageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
