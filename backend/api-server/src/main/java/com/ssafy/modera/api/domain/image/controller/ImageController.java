package com.ssafy.modera.api.domain.image.controller;

import com.ssafy.modera.api.domain.image.dto.ImageRegisterRequest;
import com.ssafy.modera.api.domain.image.dto.ImageRegisterResponse;
import com.ssafy.modera.api.domain.image.service.ImageRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageRegistrationService imageRegistrationService;

    // TODO: JWT 인증 도입 후 SecurityContext에서 userId를 추출하도록 교체하고
    // 이 헤더 기반 임시 인증은 제거한다. SecurityConfig의 permitAll도 함께 걷어낼 것.
    @PostMapping
    public ResponseEntity<ImageRegisterResponse> register(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid ImageRegisterRequest request
    ) {
        return ResponseEntity.ok(imageRegistrationService.register(userId, request));
    }
}
