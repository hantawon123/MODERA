package com.ssafy.modera.api.domain.document.controller;

import com.ssafy.modera.api.domain.document.dto.request.DocumentCreateRequest;
import com.ssafy.modera.api.domain.document.dto.response.DocumentGenerationAcceptedResponse;
import com.ssafy.modera.api.domain.document.service.DocumentCommandService;
import com.ssafy.modera.api.global.response.ApiResponse;
import com.ssafy.modera.api.global.response.ApiV1Controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "문서", description = "선택한 이미지로 마크다운 문서 생성")
@ApiV1Controller
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class DocumentController {

    private static final String CODE_ACCEPTED = "DOCUMENT_GENERATION_ACCEPTED";
    private static final String MESSAGE_ACCEPTED = "문서 생성 요청이 접수되었습니다.";

    private final DocumentCommandService documentCommandService;

    @Operation(
            summary = "문서 생성",
            description = """
                    선택한 이미지의 분석 정보를 기반으로 AI 문서 생성을 요청한다. 실제 생성과
                    저장은 비동기라 이 응답은 접수만 알린다 — 완료되면 별도 알림으로 documentId를
                    받아 상세 조회를 호출한다.

                    imageIds 순서는 그대로 유지되며 첫 번째가 중심 자료로 쓰인다. 분석이 끝나지
                    않은 이미지는 문서에 넣을 내용이 없어 요청 단계에서 거부한다.

                    clientRequestId 기준으로 중복을 막는다 — 같은 값으로 다시 호출하면 새 요청을
                    만들지 않고 409로 끊는다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "접수 성공, data에 clientRequestId/status"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "imageIds가 비었거나 중복·개수 초과(INVALID_DOCUMENT_IMAGES)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "accessToken 없음/무효(UNAUTHORIZED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아닌 이미지 포함(DOCUMENT_IMAGE_NOT_OWNED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "분석 미완료 이미지 포함 또는 중복 요청")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<DocumentGenerationAcceptedResponse>> create(
            @AuthenticationPrincipal Integer userId,
            @RequestBody @Valid DocumentCreateRequest request
    ) {
        DocumentGenerationAcceptedResponse response = documentCommandService.create(userId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(CODE_ACCEPTED, MESSAGE_ACCEPTED, response));
    }
}
