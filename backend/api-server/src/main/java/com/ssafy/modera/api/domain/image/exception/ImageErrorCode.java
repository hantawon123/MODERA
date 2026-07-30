package com.ssafy.modera.api.domain.image.exception;

import com.ssafy.modera.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "이미지를 찾을 수 없습니다."),
    IMAGE_ANALYSIS_NOT_COMPLETED(
            HttpStatus.CONFLICT,
            "IMAGE_ANALYSIS_NOT_COMPLETED",
            "이미지 분석이 완료되지 않았습니다."
    ),
    DUPLICATE_IMAGE(HttpStatus.CONFLICT, "DUPLICATE_IMAGE", "이미 등록된 이미지입니다."),
    UPLOAD_ALREADY_COMPLETED(HttpStatus.CONFLICT, "UPLOAD_ALREADY_COMPLETED", "이미지 업로드가 이미 완료되었습니다."),
    ANALYSIS_IN_PROGRESS(HttpStatus.CONFLICT, "ANALYSIS_IN_PROGRESS", "이미지 분석이 이미 시작되었습니다."),
    AI_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_SEARCH_FAILED", "AI 이미지 검색에 실패했습니다."),
    AI_SEARCH_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI_SEARCH_TIMEOUT", "AI 이미지 검색 응답 시간이 초과되었습니다."),
    CATEGORY_REANALYSIS_UNAVAILABLE(
            HttpStatus.CONFLICT,
            "CATEGORY_REANALYSIS_UNAVAILABLE",
            "카테고리 재분석을 요청할 수 없는 상태입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
