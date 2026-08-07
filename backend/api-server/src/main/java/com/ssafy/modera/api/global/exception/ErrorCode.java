package com.ssafy.modera.api.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 모든 에러 코드 enum이 구현하는 계약. {@code BusinessException}, {@code ApiResponse},
 * {@code GlobalExceptionHandler}는 이 타입만 알고 있으므로 도메인은 자기 enum을
 * 자유롭게 추가할 수 있다.
 * <p>
 * 에러코드는 소유 도메인의 enum에만 추가한다(예: 회원·인증 관련은 {@code UserErrorCode},
 * 이미지 관련은 {@code ImageErrorCode}). 어느 도메인에도 속하지 않는 공통/인프라
 * 오류만 {@link GlobalErrorCode}에 둔다. code 문자열은 전체 enum을 통틀어 중복되면
 * 안 된다({@link #getCode()} 참고, backend-conventions.md 4절).
 */
public interface ErrorCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
