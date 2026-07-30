package com.ssafy.modera.api.domain.document.exception;

import com.ssafy.modera.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 문서 도메인 오류. 이미지 자체의 오류(IMAGE_NOT_FOUND, IMAGE_ANALYSIS_NOT_COMPLETED)는
 * ImageErrorCode를 재사용하고, 여기에는 "문서 생성 요청"의 오류만 둔다.
 */
@Getter
@RequiredArgsConstructor
public enum DocumentErrorCode implements ErrorCode {

    /** imageIds가 비었거나 중복이 있거나 상한(AI가 한 번에 처리 가능한 장수)을 넘었다. */
    INVALID_DOCUMENT_IMAGES(
            HttpStatus.BAD_REQUEST,
            "INVALID_DOCUMENT_IMAGES",
            "문서 생성에 사용할 이미지 목록이 올바르지 않습니다."
    ),

    /**
     * 요청한 이미지 중 사용자 소유가 아닌 것이 있다. 존재 여부를 따로 알려주지 않고
     * 403으로 묶는다 — 남의 imageId를 넣어보며 존재를 탐색하는 걸 막는다.
     */
    DOCUMENT_IMAGE_NOT_OWNED(
            HttpStatus.FORBIDDEN,
            "DOCUMENT_IMAGE_NOT_OWNED",
            "본인이 소유한 이미지만 문서로 만들 수 있습니다."
    ),

    /** 같은 clientRequestId로 이미 접수된 요청이 있다. 재발행하지 않고 차단한다. */
    DUPLICATE_CLIENT_REQUEST(
            HttpStatus.CONFLICT,
            "DUPLICATE_CLIENT_REQUEST",
            "이미 접수된 문서 생성 요청입니다."
    ),

    /**
     * 존재하지 않거나 본인 소유가 아닌 documentId. 이미지 단건 조회와 같은 이유로 403이
     * 아니라 404로 묶는다 — 남의 documentId를 넣어보며 존재를 탐색하는 걸 막는다.
     */
    DOCUMENT_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "DOCUMENT_NOT_FOUND",
            "문서를 찾을 수 없습니다."
    ),

    /**
     * 같은 문서에 대한 재분석이 아직 진행 중이다. 두 요청이 겹치면 나중에 도착한 완료
     * 이벤트가 앞선 결과를 덮어써 사용자가 무엇을 보게 될지 예측할 수 없다.
     */
    DOCUMENT_REGENERATION_IN_PROGRESS(
            HttpStatus.CONFLICT,
            "DOCUMENT_REGENERATION_IN_PROGRESS",
            "이미 재분석이 진행 중인 문서입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
