package com.ssafy.modera.api.domain.document.controller;

import com.ssafy.modera.api.domain.document.dto.request.DocumentCreateRequest;
import com.ssafy.modera.api.domain.document.dto.request.DocumentRegenerateRequest;
import com.ssafy.modera.api.domain.document.dto.response.DocumentDeleteResponse;
import com.ssafy.modera.api.domain.document.dto.response.DocumentDetailResponse;
import com.ssafy.modera.api.domain.document.dto.response.DocumentImageResponse;
import com.ssafy.modera.api.domain.document.dto.response.DocumentSummaryResponse;
import com.ssafy.modera.api.domain.document.service.DocumentCommandService;
import com.ssafy.modera.api.domain.document.service.DocumentDeleteService;
import com.ssafy.modera.api.domain.document.service.DocumentQueryService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "문서", description = "선택한 이미지로 마크다운 문서 생성")
@ApiV1Controller
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class DocumentController {

    private static final String CODE_CREATED = "DOCUMENT_GENERATION_COMPLETED";
    private static final String MESSAGE_CREATED = "문서가 생성되었습니다.";
    private static final String MESSAGE_REGENERATED = "문서를 다시 정리했습니다.";

    private final DocumentCommandService documentCommandService;
    private final DocumentQueryService documentQueryService;
    private final DocumentDeleteService documentDeleteService;

    @Operation(
            summary = "문서 생성",
            description = """
                    선택한 이미지의 분석 정보로 AI 문서를 만들어 **완성된 문서를 그대로 돌려준다.**
                    생성이 끝날 때까지 응답이 나가지 않으므로 폴링이나 완료 알림이 필요 없다.

                    LLM 생성이 수 초~수십 초 걸린다. 클라이언트 read 타임아웃을 넉넉히(90초 이상)
                    잡아야 하고, 그동안 로딩 화면을 유지하면 된다.

                    imageIds 순서는 그대로 유지되며 첫 번째가 중심 자료로 쓰인다. 분석이 끝나지
                    않은 이미지는 문서에 넣을 내용이 없어 요청 단계에서 거부한다.

                    clientRequestId 기준으로 중복을 막는다 — 같은 값으로 다시 호출하면 새로 만들지
                    않고 409로 끊는다. 재시도가 아니라 새 문서를 만들려면 새 UUID를 쓴다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 완료. data는 상세 조회(8-3)와 같은 형식"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "imageIds가 비었거나 중복·개수 초과(INVALID_DOCUMENT_IMAGES)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아닌 이미지 포함(DOCUMENT_IMAGE_NOT_OWNED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "분석 미완료 이미지 포함 또는 중복 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 장애·타임아웃(DOCUMENT_GENERATION_FAILED) 또는 요청 거부(DOCUMENT_AI_REJECTED)")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> create(
            @AuthenticationPrincipal Integer userId,
            @RequestBody @Valid DocumentCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                CODE_CREATED,
                MESSAGE_CREATED,
                documentCommandService.create(userId, request)
        ));
    }

    @Operation(
            summary = "내 문서 목록 조회",
            description = """
                    삭제되지 않은 본인 문서를 페이지로 조회한다. 목록에는 마크다운 본문이 실리지
                    않는다 — 본문은 상세 조회에서 준다.

                    delImageCount가 0이 아니면 문서를 만든 뒤 원본 이미지가 삭제된 문서다(재분석 대상).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "page/size/sort 값이 올바르지 않음(INVALID_PARAMETER)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DocumentSummaryResponse>>> getDocuments(
            @AuthenticationPrincipal Integer userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "UPDATED_DESC, UPDATED_ASC, NAME_ASC")
            @RequestParam(name = "sort", defaultValue = "UPDATED_DESC") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "D201",
                documentQueryService.getDocuments(userId, page, size, sort)
        ));
    }

    @Operation(
            summary = "문서 상세 조회",
            description = """
                    마크다운 본문 전문과 구성 이미지 ID를 함께 준다.

                    regenerating이 true면 재분석이 진행 중이다 — 완료 알림이 아직 없어서, 재분석이
                    끝났는지 확인하는 창구가 이 조회다. 재분석이 실패해도 문서 내용은 이전 상태
                    그대로 남는다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 존재하지 않는 documentId(DOCUMENT_NOT_FOUND)")
    })
    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> getDocument(
            @AuthenticationPrincipal Integer userId,
            @Parameter(description = "조회할 문서 ID") @PathVariable(name = "documentId") Integer documentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "D203",
                documentQueryService.getDocument(userId, documentId)
        ));
    }

    @Operation(
            summary = "문서 구성 이미지 목록 조회",
            description = """
                    이 문서가 어떤 스크린샷으로 만들어졌는지 조회한다. 삭제된 이미지는 빠진다.

                    정렬은 문서에 포함된 시각 기준이다(업로드 시각은 이 조회 모델에 없다).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공. 구성 이미지가 없으면 빈 배열"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "page/size/sort 값이 올바르지 않음(INVALID_PARAMETER)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 존재하지 않는 documentId(DOCUMENT_NOT_FOUND)")
    })
    @GetMapping("/{documentId}/images")
    public ResponseEntity<ApiResponse<PageResponse<DocumentImageResponse>>> getDocumentImages(
            @AuthenticationPrincipal Integer userId,
            @PathVariable(name = "documentId") Integer documentId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @Parameter(description = "ADDED_DESC, ADDED_ASC, TITLE_ASC")
            @RequestParam(name = "sort", defaultValue = "ADDED_DESC") String sort
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "D204",
                documentQueryService.getDocumentImages(userId, documentId, page, size, sort)
        ));
    }

    @Operation(
            summary = "문서 재분석",
            description = """
                    같은 문서를 다시 만들고 **갱신된 문서를 그대로 돌려준다.** 생성과 마찬가지로
                    완료될 때까지 응답이 나가지 않는다.

                    새 문서가 생기지 않고 documentId가 그대로 유지되므로 앱이 보고 있는 화면과
                    링크가 살아 있고, 실패해도(502) 이전 문서 내용이 그대로 남는다.

                    imageIds를 생략하면 현재 구성 이미지로 내용만 다시 정리한다. 값을 주면 그것이
                    최종 구성이 되므로 이미지 추가·제외도 이 API 하나로 처리한다.

                    한 문서에 동시에 두 번 재분석할 수는 없다(409).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재분석 완료. data는 상세 조회(8-3)와 같은 형식"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "imageIds가 중복·개수 초과이거나 남는 이미지가 없음(INVALID_DOCUMENT_IMAGES)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아닌 이미지 포함(DOCUMENT_IMAGE_NOT_OWNED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 존재하지 않는 documentId(DOCUMENT_NOT_FOUND)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "분석 미완료 이미지 포함, 중복 요청, 또는 이미 재분석 진행 중"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 장애·타임아웃(DOCUMENT_GENERATION_FAILED) 또는 요청 거부(DOCUMENT_AI_REJECTED)")
    })
    @PostMapping("/{documentId}/regenerate")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> regenerate(
            @AuthenticationPrincipal Integer userId,
            @PathVariable(name = "documentId") Integer documentId,
            @RequestBody @Valid DocumentRegenerateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                CODE_CREATED,
                MESSAGE_REGENERATED,
                documentCommandService.regenerate(userId, documentId, request)
        ));
    }

    @Operation(
            summary = "문서 삭제",
            description = """
                    문서 원본·관계·조회 모델을 soft delete한다. 문서를 만드는 데 쓰인 이미지는
                    삭제되지 않는다 — 다른 문서에도 더 이상 쓰이지 않는 이미지만 문서화 표시가 꺼진다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "본인 소유가 아니거나 이미 삭제된 documentId(DOCUMENT_NOT_FOUND)")
    })
    @DeleteMapping("/{documentId}")
    public ResponseEntity<ApiResponse<DocumentDeleteResponse>> deleteDocument(
            @AuthenticationPrincipal Integer userId,
            @PathVariable(name = "documentId") Integer documentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "D205",
                "문서가 삭제되었습니다.",
                documentDeleteService.delete(userId, documentId)
        ));
    }
}
