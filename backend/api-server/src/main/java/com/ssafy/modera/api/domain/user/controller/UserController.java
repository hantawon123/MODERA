package com.ssafy.modera.api.domain.user.controller;

import com.ssafy.modera.api.domain.user.dto.response.UserDataDeleteResponse;
import com.ssafy.modera.api.domain.user.dto.response.UserInfoResponse;
import com.ssafy.modera.api.domain.user.service.UserCommandService;
import com.ssafy.modera.api.domain.user.service.UserQueryService;
import com.ssafy.modera.api.global.response.ApiResponse;
import com.ssafy.modera.api.global.response.ApiV1Controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자", description = "사용자 정보와 설정")
@ApiV1Controller
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;

    @Operation(
            summary = "내 정보 조회",
            description = "인증된 사용자의 계정 정보와 알림·백그라운드 분석 설정을 조회합니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "사용자 없음(USER_NOT_FOUND)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo(
            @AuthenticationPrincipal Integer userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "U201",
                userQueryService.getMyInfo(userId)
        ));
    }

    @Operation(
            summary = "저장 데이터 초기화",
            description = "계정과 설정은 유지하고 사용자별 관계·조회·요청 이력을 soft delete합니다. " +
                    "공유 원본 객체와 MinIO/S3 파일은 삭제하지 않으며 요청 본문은 없습니다."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "초기화 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "사용자 없음(USER_NOT_FOUND)")
    })
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<UserDataDeleteResponse>> deleteStoredData(
            @AuthenticationPrincipal Integer userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "U202",
                userCommandService.deleteStoredData(userId)
        ));
    }
}
