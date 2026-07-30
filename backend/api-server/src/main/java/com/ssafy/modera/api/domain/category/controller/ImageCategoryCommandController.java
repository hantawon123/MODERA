package com.ssafy.modera.api.domain.category.controller;

import com.ssafy.modera.api.domain.category.dto.response.CategoryReanalysisResponse;
import com.ssafy.modera.api.domain.category.service.CategoryCommandService;
import com.ssafy.modera.api.global.response.ApiResponse;
import com.ssafy.modera.api.global.response.ApiV1Controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이미지 카테고리", description = "사용자별 이미지 카테고리 재분류")
@ApiV1Controller
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ImageCategoryCommandController {

    private final CategoryCommandService categoryCommandService;

    @Operation(
            summary = "이미지 재분석(카테고리 재분류)",
            description = """
                    본인이 등록한 분석 완료 이미지를 AI가 다시 분류하도록 요청한다.
                    사용자의 최근 카테고리 결과를 최대 5개까지 제외 목록으로 전달하며,
                    요청은 비동기로 처리된다. 완료된 결과는 요청한 사용자의 이미지에만 반영된다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "재분류 요청 접수"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "분석 미완료, 재분류 진행 중 또는 재분류할 수 없는 이미지"
            )
    })
    @PostMapping("/{imageId}/category/reanalysis")
    public ResponseEntity<ApiResponse<CategoryReanalysisResponse>> requestReanalysis(
            @AuthenticationPrincipal Integer userId,
            @Parameter(description = "재분류할 이미지 ID", required = true)
            @PathVariable(name = "imageId") Integer imageId
    ) {
        CategoryReanalysisResponse response =
                categoryCommandService.request(userId, imageId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("I212", response));
    }
}
