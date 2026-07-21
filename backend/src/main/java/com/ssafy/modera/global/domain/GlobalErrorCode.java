package com.ssafy.modera.global.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 어느 도메인에도 속하지 않는 공통 에러 코드.
 * <p>
 * prefix 는 <b>G</b>(HTTP·서버 공통)와 <b>A</b>(인증/인가 인프라)만 사용한다.
 * 도메인 비즈니스 에러는 여기 추가하지 말고 해당 도메인의 enum 에 넣는다. ({@link ErrorCode} 레지스트리 참고)
 */
@RequiredArgsConstructor
@Getter
public enum GlobalErrorCode implements ErrorCode {

    // Global (공통, G-xxx)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G001", "예상치 못한 서버 오류입니다. 관리자에게 문의해주세요."),
    EXTERNAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "G002", "외부 서비스와의 통신 중 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "G003", "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G004", "잘못된 입력입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "G005", "허용되지 않은 HTTP 메소드입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "G006", "요청하신 리소스를 찾을 수 없습니다."),

    // Auth 인프라 (A-xxx) — 전역 시큐리티 계층(필터·핸들러·JwtTokenValidator)이 던진다.
    // 로그인 실패·리프레시 토큰 같은 '비즈니스' 인증 에러는 UserErrorCode(U-xxx) 소관이다.
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    JWT_MALFORMED(HttpStatus.UNAUTHORIZED, "A003", "잘못된 JWT 토큰입니다."),
    JWT_EXPIRED(HttpStatus.UNAUTHORIZED, "A004", "만료된 JWT 토큰입니다."),
    JWT_INVALID(HttpStatus.UNAUTHORIZED, "A005", "유효하지 않은 JWT 토큰입니다."),
    JWT_MISSING(HttpStatus.UNAUTHORIZED, "A006", "JWT 토큰이 없습니다."),
    // A007·A008 은 UserErrorCode 로 이관됨 (영구 결번, 재사용 금지)
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "A009", "요청 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요."),
    // A010~A023 은 UserErrorCode 로 이관됨 (영구 결번, 재사용 금지)
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
