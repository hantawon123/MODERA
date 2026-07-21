package com.ssafy.modera.domain.user.exception;

import com.ssafy.modera.global.domain.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 회원·인증 도메인 에러 코드. prefix 는 <b>U</b> 만 사용한다. ({@link ErrorCode} 레지스트리 참고)
 * <p>
 * JWT 파싱 실패처럼 전역 시큐리티 계층이 던지는 인프라 에러는 여기가 아니라
 * {@code GlobalErrorCode}(A-xxx) 소관이다.
 */
@RequiredArgsConstructor
@Getter
public enum UserErrorCode implements ErrorCode {

    // 계정 (가입·프로필)
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U001", "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U002", "이미 존재하는 닉네임입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "U003", "비밀번호가 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "U004", "일치하는 회원 정보가 존재하지 않습니다."),
    SOCIAL_USER_CANNOT_CHANGE_PASSWORD(HttpStatus.BAD_REQUEST, "U005", "소셜 로그인 사용자는 비밀번호 변경이 불가능합니다."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "U006", "현재 비밀번호가 일치하지 않습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "U007", "이미 사용 중인 로그인 ID입니다."),

    // 로그인 (3-2)
    // 계정 열거(account enumeration) 방지를 위해 ID 오류와 비밀번호 오류를 구분하지 않는다.
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U008", "아이디 또는 비밀번호가 올바르지 않습니다."),

    // RTR(Refresh Token Rotation) — 3-3 재발급 / 3-4 로그아웃
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "U009", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "U010", "리프레시 토큰을 찾을 수 없습니다. (로그아웃 되었습니다)"),
    REFRESH_TOKEN_THEFT_DETECTED(HttpStatus.UNAUTHORIZED, "U011", "토큰 탈취가 감지되었습니다. 보안을 위해 재로그인이 필요합니다."),

    // 소셜 로그인 (미구현)
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U012", "소셜 로그인에 실패했습니다."),
    INVALID_PROVIDER(HttpStatus.BAD_REQUEST, "U013", "지원하지 않는 소셜 로그인 제공자입니다."),

    // 이메일 인증·비밀번호 재설정 (미구현)
    AUTH_CODE_INVALID(HttpStatus.BAD_REQUEST, "U014", "유효하지 않은 인증 코드입니다."),
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "U015", "유효하지 않은 비밀번호 재설정 토큰입니다."),
    VERIFICATION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "U016", "이미 인증이 완료된 상태입니다."),
    VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "U017", "인증 시간이 만료되었습니다. 다시 시도해주세요."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "U018", "인증 코드가 일치하지 않습니다."),
    VERIFICATION_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "U019", "인증되지 않은 상태입니다."),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "U020", "인증 정보를 찾을 수 없습니다."),
    INVALID_VERIFICATION_PURPOSE(HttpStatus.BAD_REQUEST, "U021", "유효하지 않은 인증 목적입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
