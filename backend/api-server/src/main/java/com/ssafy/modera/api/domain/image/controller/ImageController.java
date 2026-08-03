package com.ssafy.modera.api.domain.image.controller;

import com.ssafy.modera.api.domain.image.dto.request.ImageDeleteRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageDocumentizeRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageFavoriteRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageRegisterRequest;
import com.ssafy.modera.api.domain.image.dto.request.ImageSemanticSearchRequest;
import com.ssafy.modera.api.domain.image.dto.response.ImageDeleteResponse;
import com.ssafy.modera.api.domain.image.dto.response.ImageDetailResponse;
import com.ssafy.modera.api.domain.image.dto.response.ImageFavoriteResponse;
import com.ssafy.modera.api.domain.image.dto.response.ImageRegisterResponse;
import com.ssafy.modera.api.domain.image.dto.response.ImageSummaryResponse;
import com.ssafy.modera.api.domain.image.dto.response.ImageListResponse;
import com.ssafy.modera.api.domain.image.dto.response.ImageUploadUrlResponse;
import com.ssafy.modera.api.domain.image.service.ImageQueryService;
import com.ssafy.modera.api.domain.image.service.ImageCommandService;
import com.ssafy.modera.api.domain.image.dto.response.SimilarImagesResponse;
import com.ssafy.modera.api.domain.image.service.ImageSimilarService;
import com.ssafy.modera.api.domain.image.service.ImageSemanticSearchService;
import com.ssafy.modera.api.global.response.ApiResponse;
import com.ssafy.modera.api.global.response.ApiV1Controller;
import com.ssafy.modera.api.global.response.PageResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "이미지", description = "이미지 등록·업로드 URL 재발급·목록·단건 조회")
@ApiV1Controller
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ImageController {

    private final ImageCommandService imageCommandService;
    private final ImageQueryService imageQueryService;
    private final ImageSimilarService imageSimilarService;
    private final ImageSemanticSearchService imageSemanticSearchService;

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
            @AuthenticationPrincipal Integer userId,
            @RequestBody @Valid ImageRegisterRequest request
    ) {
        ImageRegisterResponse response = imageCommandService.register(userId, request);
        return ResponseEntity.ok(ApiResponse.success("I201", response));
    }

    @Operation(summary = "이미지 업로드 URL 재발급", description = "업로드 전 만료된 Presigned PUT URL을 재발급한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "활성 이미지 관계 없음(IMAGE_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "업로드 완료 또는 분석 시작")
    })
    @PostMapping("/{imageId}/upload-url")
    public ResponseEntity<ApiResponse<ImageUploadUrlResponse>> reissueUploadUrl(
            @AuthenticationPrincipal Integer userId,
            @PathVariable(name = "imageId") Integer imageId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "I204",
                imageCommandService.reissueUploadUrl(userId, imageId)
        ));
    }

    @Operation(summary = "이미지 목록 조회", description = "활성 이미지 목록을 필터링·정렬하여 페이지로 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ImageListResponse>> getImages(
            @AuthenticationPrincipal Integer userId,
            @RequestParam(name = "favorite", required = false) Boolean favorite,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "UPLOADED_DESC") String sort,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Integer categoryId
    ) {
        ImageListResponse response = imageQueryService.getImages(
                userId, favorite, page, size, sort, keyword, categoryId);
        return ResponseEntity.ok(ApiResponse.success("I205", response));
    }

    @Operation(
            summary = "이미지 단건 조회",
            description = "본인이 등록한 이미지만 조회할 수 있다. 다른 사용자 소유 이미지는 존재 " +
                    "자체를 숨기기 위해 403이 아니라 404(IMAGE_NOT_FOUND)로 응답한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 존재하지 않는 imageId(IMAGE_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미지 분석이 완료되지 않음(IMAGE_ANALYSIS_NOT_COMPLETED)")
    })
    @GetMapping("/{imageId}")
    public ResponseEntity<ApiResponse<ImageDetailResponse>> getImage(
            @AuthenticationPrincipal Integer userId,
            @Parameter(description = "조회할 이미지 ID")
            @PathVariable(name = "imageId") Integer imageId
    ) {
        ImageDetailResponse response = imageQueryService.getImage(userId, imageId);
        return ResponseEntity.ok(ApiResponse.success("I206", response));
    }

    @Operation(
            summary = "이미지 선택 삭제",
            description = "하나 이상의 이미지 ID를 받아 사용자별 이미지 관계와 조회 데이터를 soft delete한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "항목별 삭제 결과 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "imageIds가 비어 있거나 100개를 초과함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)")
    })
    @DeleteMapping
    public ResponseEntity<ApiResponse<ImageDeleteResponse>> deleteImages(
            @AuthenticationPrincipal Integer userId,
            @RequestBody @Valid ImageDeleteRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "I207",
                imageCommandService.deleteImages(userId, request)
        ));
    }

    @Operation(
            summary = "즐겨찾기 설정/해제",
            description = "이미지의 사용자별 즐겨찾기 여부를 변경하고 변경 직후의 전체 즐겨찾기 개수를 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "즐겨찾기 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "favorite 누락 또는 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 삭제된 이미지(IMAGE_NOT_FOUND)")
    })
    @PutMapping("/{imageId}/favorite")
    public ResponseEntity<ApiResponse<ImageFavoriteResponse>> updateFavorite(
            @AuthenticationPrincipal Integer userId,
            @Parameter(description = "즐겨찾기를 변경할 이미지 ID")
            @PathVariable(name = "imageId") Integer imageId,
            @RequestBody @Valid ImageFavoriteRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "I208",
                imageCommandService.updateFavorite(userId, imageId, request)
        ));
    }

    @Operation(
            summary = "자연어 기반 AI 이미지 검색",
            description = "검색어를 Analysis Worker에 이벤트로 전달하고 유사도순 이미지 목록을 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "검색어 또는 페이지 파라미터 오류(INVALID_PARAMETER)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "AI 검색 실패(AI_SEARCH_FAILED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "AI 검색 시간 초과(AI_SEARCH_TIMEOUT)")
    })
    @PostMapping("/search/semantic")
    public ResponseEntity<ApiResponse<PageResponse<ImageSummaryResponse>>> searchSemantic(
            @AuthenticationPrincipal Integer userId,
            @RequestBody @Valid ImageSemanticSearchRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "I209",
                imageSemanticSearchService.search(userId, request)
        ));
    }

    @Operation(
            summary = "연관 이미지 조회",
            description = """
                    기준 이미지와 내용이 비슷한 본인 이미지를 유사도 내림차순으로 반환한다.
                    유사도는 analysis-worker가 임베딩으로 계산하고, 화면에 뿌릴 메타데이터는
                    api-server가 붙인다.

                    연관 자료는 부가 정보라 worker 호출이 실패해도 500이 아니라
                    빈 list(count=0)로 응답한다 — 상세 화면 전체가 실패하지 않게 하는 쪽을 택했다.
                    분석이 안 된 이미지, 임베딩이 없는 이미지도 같은 이유로 빈 list다.

                    limit은 1~50 범위로 조정되며(범위를 벗어나면 400이 아니라 경계값으로 당긴다),
                    삭제되었거나 read model이 아직 없는 이미지는 결과에서 빠지므로
                    count가 limit보다 작을 수 있다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(연관 이미지가 없으면 빈 list)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 존재하지 않는 imageId(IMAGE_NOT_FOUND)")
    })
    @GetMapping("/{imageId}/similar")
    public ResponseEntity<ApiResponse<SimilarImagesResponse>> getSimilarImages(
            @AuthenticationPrincipal Integer userId,
            @Parameter(description = "기준 이미지 ID")
            @PathVariable(name = "imageId") Integer imageId,
            @Parameter(description = "최대 개수(1~50)")
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        SimilarImagesResponse response = imageSimilarService.getSimilarImages(userId, imageId, limit);
        return ResponseEntity.ok(ApiResponse.success("I210", response));
    }

    @Operation(
            summary = "다중 이미지 문서화 관련 자료 검색",
            description = """
                    문서로 만들려고 고른 기준 이미지들과 내용이 비슷한 다른 이미지를 추천한다.
                    이미지 선택 화면의 "이것도 관련 있어요" 목록을 채우는 용도다 — 문서를 만들지
                    않으며, 몇 번을 불러도 조회만 일어난다.

                    기준 이미지들의 임베딩 평균(centroid)으로 검색하므로 개별 이미지가 아니라
                    공통 주제에 가까운 것이 잡힌다. 유사도 0.6 미만은 제외되어 결과가 10개보다
                    적거나 빈 배열일 수 있다. 페이지는 0/10 고정이고 페이징을 지원하지 않는다.

                    검색 서버 장애 시에는 에러 대신 빈 목록을 반환한다(추천이 안 떠도 선택
                    화면은 동작해야 한다).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공. 추천이 없으면 빈 list. 항목은 5-1 목록 DTO와 동일"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "imageIds가 비었거나 중복(INVALID_PARAMETER)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 존재하지 않는 이미지 포함(IMAGE_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "분석 미완료 이미지 포함(IMAGE_ANALYSIS_NOT_COMPLETED)")
    })
    @PostMapping("/documentize")
    public ResponseEntity<ApiResponse<PageResponse<ImageSummaryResponse>>> documentize(
            @AuthenticationPrincipal Integer userId,
            @RequestBody @Valid ImageDocumentizeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "I211",
                imageSimilarService.findDocumentizeCandidates(userId, request.imageIds())
        ));
    }
}
