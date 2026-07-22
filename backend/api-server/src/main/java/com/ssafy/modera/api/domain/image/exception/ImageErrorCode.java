package com.ssafy.modera.api.domain.image.exception;

import com.ssafy.modera.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "이미지를 찾을 수 없습니다."),
    DUPLICATE_IMAGE(HttpStatus.CONFLICT, "DUPLICATE_IMAGE", "이미 등록된 이미지입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
