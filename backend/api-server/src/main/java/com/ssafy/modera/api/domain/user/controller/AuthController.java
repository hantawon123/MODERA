package com.ssafy.modera.api.domain.user.controller;

import com.ssafy.modera.api.domain.user.dto.request.LoginRequest;
import com.ssafy.modera.api.domain.user.dto.request.LogoutRequest;
import com.ssafy.modera.api.domain.user.dto.request.RefreshRequest;
import com.ssafy.modera.api.domain.user.dto.request.RegisterRequest;
import com.ssafy.modera.api.domain.user.dto.response.LogoutResponse;
import com.ssafy.modera.api.domain.user.dto.response.RegisterResponse;
import com.ssafy.modera.api.domain.user.dto.response.TokenResponse;
import com.ssafy.modera.api.domain.user.service.AuthService;
import com.ssafy.modera.api.global.response.ApiResponse;
import com.ssafy.modera.api.global.response.ApiV1Controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "회원가입 · 로그인 · 토큰 재발급(RTR) · 로그아웃")
@ApiV1Controller
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "회원가입",
            description = "loginId는 4~20자, password는 8자 이상이어야 한다. loginId 또는 email이 이미 " +
                    "쓰이고 있으면 각각 DUPLICATE_LOGIN_ID / DUPLICATE_EMAIL로 실패한다. 토큰 없이 호출한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "가입 성공, data.userId 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증 실패(길이 제약 위반 등) — data에 필드별 오류 배열"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "loginId 또는 email 중복(DUPLICATE_LOGIN_ID / DUPLICATE_EMAIL)")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request)));
    }

    @Operation(
            summary = "로그인",
            description = "deviceId 단위로 refreshToken을 관리한다(같은 기기로 다시 로그인하면 기존 " +
                    "refreshToken은 새 값으로 교체된다). 존재하지 않는 loginId와 비밀번호 불일치를 " +
                    "구분하지 않고 LOGIN_FAILED로 통합해 계정 열거 공격을 막는다. 토큰 없이 호출한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공, data에 accessToken/refreshToken/userId"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "loginId 없음 또는 비밀번호 불일치(LOGIN_FAILED)")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @Operation(
            summary = "토큰 재발급 (Refresh Token Rotation)",
            description = "제시한 refreshToken을 검증하고 새 accessToken/refreshToken 쌍으로 교체한다. " +
                    "제시한 refreshToken은 이 호출 이후 즉시 무효화되므로, 같은 refreshToken을 두 번 쓰면 " +
                    "(회전 전 값 재사용) 실패한다 — 서명 무효/만료/deviceId 불일치/이미 회전된 옛 토큰 재사용 " +
                    "전부 INVALID_REFRESH_TOKEN으로 통합된다. 토큰 없이 호출한다(refreshToken 자체가 인증 수단)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공, data에 새 accessToken/refreshToken/userId"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "무효/만료/재사용된 refreshToken(INVALID_REFRESH_TOKEN)")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody @Valid RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request)));
    }

    @Operation(
            summary = "로그아웃",
            description = "Authorization 헤더의 accessToken으로 본인 확인 후, 요청 본문의 deviceId에 " +
                    "해당하는 refreshToken 레코드만 삭제한다(다른 기기의 로그인 세션은 유지됨). " +
                    "제시한 refreshToken이 실제 저장된 값과 다르면 실패한다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공, data.loggedOut=true"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED) 또는 refreshToken/deviceId 불일치(INVALID_REFRESH_TOKEN)")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @AuthenticationPrincipal Integer userId,
            @RequestBody @Valid LogoutRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(authService.logout(userId, request)));
    }
}
