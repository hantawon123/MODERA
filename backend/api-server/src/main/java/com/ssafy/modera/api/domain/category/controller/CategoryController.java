package com.ssafy.modera.api.domain.category.controller;

import com.ssafy.modera.api.domain.category.dto.response.CategoryListResponse;
import com.ssafy.modera.api.domain.category.service.CategoryQueryService;
import com.ssafy.modera.api.global.response.ApiResponse;
import com.ssafy.modera.api.global.response.ApiV1Controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "카테고리", description = "카테고리 목록 조회")
@ApiV1Controller
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class CategoryController {

    private final CategoryQueryService categoryQueryService;

    @Operation(
            summary = "카테고리 목록 조회",
            description = """
                    로그인한 사용자의 카테고리 카드(이름·대표 이미지·활성 이미지 수·최신 업로드
                    시각)를 전체 목록으로 반환한다. 페이지네이션 없이 한 번에 내려간다.

                    sort는 다른 목록 API(5-1, 8-1, 9-1)와 같은 enum 방식이며 대소문자를
                    구분하지 않는다: NAME_ASC(이름순, 기본) / UPDATED_DESC(최신 업로드순) /
                    IMAGE_COUNT_DESC(사진 많은 순). 그 외 값은 INVALID_PARAMETER(400)다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 — data.list에 카테고리 카드 배열"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 sort 값(INVALID_PARAMETER)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<CategoryListResponse>> getCategories(
            @AuthenticationPrincipal Integer userId,
            @Parameter(
                    description = "정렬 기준. NAME_ASC(이름순, 기본) | UPDATED_DESC(최신 업로드순) | IMAGE_COUNT_DESC(사진 많은 순)",
                    schema = @Schema(
                            allowableValues = {"NAME_ASC", "UPDATED_DESC", "IMAGE_COUNT_DESC"},
                            defaultValue = "NAME_ASC"))
            @RequestParam(name = "sort", defaultValue = "NAME_ASC") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "T202",
                categoryQueryService.getCategories(userId, sort)
        ));
    }
}
