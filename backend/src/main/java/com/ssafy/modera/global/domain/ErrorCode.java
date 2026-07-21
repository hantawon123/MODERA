package com.ssafy.modera.global.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    // Global (공통, G-xxx)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G001", "예상치 못한 서버 오류입니다. 관리자에게 문의해주세요."),
    EXTERNAL_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "G002", "외부 서비스와의 통신 중 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "G003", "잘못된 요청입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "G004", "잘못된 입력입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "G005", "허용되지 않은 HTTP 메소드입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "G006", "요청하신 리소스를 찾을 수 없습니다."),

    // User (사용자, U-xxx)
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U001", "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U002", "이미 존재하는 닉네임입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "U003", "비밀번호가 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "U004", "일치하는 회원 정보가 존재하지 않습니다."),
    SOCIAL_USER_CANNOT_CHANGE_PASSWORD(HttpStatus.BAD_REQUEST, "U005", "소셜 로그인 사용자는 비밀번호 변경이 불가능합니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "U006", "현재 비밀번호가 일치하지 않습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "U007", "이미 사용 중인 로그인 ID입니다."),

    // Auth (인증/인가, A-xxx)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "접근 권한이 없습니다."),
    JWT_MALFORMED(HttpStatus.UNAUTHORIZED, "A003", "잘못된 JWT 토큰입니다."),
    JWT_EXPIRED(HttpStatus.UNAUTHORIZED, "A004", "만료된 JWT 토큰입니다."),
    JWT_INVALID(HttpStatus.UNAUTHORIZED, "A005", "유효하지 않은 JWT 토큰입니다."),
    JWT_MISSING(HttpStatus.UNAUTHORIZED, "A006", "JWT 토큰이 없습니다."),
    AUTH_CODE_INVALID(HttpStatus.BAD_REQUEST, "A007", "유효하지 않은 인증 코드입니다."),
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "A008", "유효하지 않은 비밀번호 재설정 토큰입니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "A009", "요청 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요."),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A010", "소셜 로그인에 실패했습니다."),
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "A011", "지원하지 않는 소셜 로그인 제공자입니다."),
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "A012", "비밀번호가 일치하지 않습니다."),
    VERIFICATION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "A013", "이미 인증이 완료된 상태입니다."),
    VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "A014", "인증 시간이 만료되었습니다. 다시 시도해주세요."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "A015", "인증 코드가 일치하지 않습니다."),
    VERIFICATION_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "A016", "인증되지 않은 상태입니다."),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "A017", "인증 정보를 찾을 수 없습니다."),
    INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, "A018", "유효하지 않은 비밀번호 재설정 토큰입니다."),
    INVALID_VERIFICATION_PURPOSE(HttpStatus.BAD_REQUEST, "A019", "유효하지 않은 인증 목적입니다."),
    // 계정 열거(account enumeration) 방지를 위해 ID 오류와 비밀번호 오류를 구분하지 않는다. (API 명세 3-2)
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A020", "아이디 또는 비밀번호가 올바르지 않습니다."),

    // RTR(Refresh Token Rotation) 관련 에러
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A021", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A022", "리프레시 토큰을 찾을 수 없습니다. (로그아웃 되었습니다)"),
    REFRESH_TOKEN_THEFT_DETECTED(HttpStatus.UNAUTHORIZED, "A023", "토큰 탈취가 감지되었습니다. 보안을 위해 재로그인이 필요합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
