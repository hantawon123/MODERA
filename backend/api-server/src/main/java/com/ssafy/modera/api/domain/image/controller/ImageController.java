package com.ssafy.modera.api.domain.image.controller;

import com.ssafy.modera.api.domain.image.dto.ImageDetailResponse;
import com.ssafy.modera.api.domain.image.dto.ImageRegisterRequest;
import com.ssafy.modera.api.domain.image.dto.ImageRegisterResponse;
import com.ssafy.modera.api.domain.image.service.ImageQueryService;
import com.ssafy.modera.api.domain.image.service.ImageRegistrationService;
import com.ssafy.modera.api.global.response.ApiResponse;
import com.ssafy.modera.api.global.response.ApiV1Controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "이미지", description = "이미지 등록(presigned URL 발급) · 단건 조회. 목록/검색 API는 아직 없음")
@ApiV1Controller
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ImageController {

    private final ImageRegistrationService imageRegistrationService;
    private final ImageQueryService imageQueryService;

    @Operation(
            summary = "이미지 등록",
            description = """
                    이미지 메타데이터를 저장하고 MinIO에 직접 업로드할 presigned PUT URL을 발급한다.
                    실제 파일은 이 API가 아니라 응답의 presignedPutUrl로 클라이언트가 직접 PUT한다
                    (multipart 아님, binary 그대로 PUT). 업로드가 끝났다는 걸 클라이언트가 따로
                    알릴 필요는 없다 — MinIO의 ObjectCreated 이벤트를 서버가 webhook으로 받아
                    upload_status를 갱신하고 분석 파이프라인을 시작시킨다.

                    clientRequestId 기준으로 멱등하다 — 같은 clientRequestId로 다시 호출하면
                    새로 만들지 않고 기존 이미지에 대한 presigned URL을 새로 발급해서 돌려준다
                    (presigned URL은 10분 후 만료되므로 재시도 시 새로 받아야 한다).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공(또는 이미 등록된 clientRequestId라 기존 건 반환), data에 imageId/presignedPutUrl/s3Key/expiresAt"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 검증 실패 — data에 필드별 오류 배열"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ImageRegisterResponse>> register(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ImageRegisterRequest request
    ) {
        ImageRegisterResponse response = imageRegistrationService.register(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "이미지 단건 조회",
            description = "본인이 등록한 이미지만 조회할 수 있다. 다른 사용자 소유 이미지는 존재 " +
                    "자체를 숨기기 위해 403이 아니라 404(IMAGE_NOT_FOUND)로 응답한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 존재하지 않는 imageId(IMAGE_NOT_FOUND)")
    })
    @GetMapping("/{imageId}")
    public ResponseEntity<ApiResponse<ImageDetailResponse>> getImage(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "조회할 이미지 UUID") @PathVariable UUID imageId
    ) {
        ImageDetailResponse response = imageQueryService.getImage(userId, imageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
