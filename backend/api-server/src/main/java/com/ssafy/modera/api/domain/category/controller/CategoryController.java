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
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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

    @Operation(
            summary = "카테고리 아이콘 조회(리다이렉트)",
            description = """
                    카테고리 아이콘(AI 생성, 216×216 PNG)의 presigned URL로 302 리다이렉트한다.

                    이 경로 자체는 만료가 없어서 앱이 Room에 영구 저장하고 이미지 캐시 키로
                    쓸 수 있다 — presigned URL의 1시간 만료는 매 호출 새로 발급하는 것으로
                    흡수된다. 목록(6-1)의 categoryImageUrl이 곧 이 경로다.

                    아이콘 생성은 AI 쪽 백그라운드(수십 초)라 카테고리 생성 직후에는
                    리다이렉트된 곳이 404일 수 있다 — 앱은 플레이스홀더를 보여주고 다음
                    로드에서 자연 복구된다.

                    302 응답은 본문이 없어 공통 envelope 예외다. 에러(401·404)는 평소처럼
                    envelope JSON으로 나간다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "Location에 presigned GET URL(1시간 유효)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 존재하지 않는 categoryId(CATEGORY_NOT_FOUND)")
    })
    @GetMapping("/{categoryId}/thumbnail")
    public ResponseEntity<Void> getCategoryThumbnail(
            @AuthenticationPrincipal Integer userId,
            @Parameter(description = "카테고리 ID") @PathVariable(name = "categoryId") Integer categoryId
    ) {
        String presignedUrl = categoryQueryService.getThumbnailRedirectUrl(userId, categoryId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                // 리다이렉트 자체를 캐시하면 만료된 presigned URL이 재사용된다.
                // 이미지 캐시는 앱이 이 경로(불변)를 키로 알아서 한다.
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
