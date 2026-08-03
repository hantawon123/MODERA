package com.ssafy.modera.api.domain.user.exception;

import com.ssafy.modera.api.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 회원·인증 도메인 에러. code 문자열 값은 현재 semantic 이름 그대로 쓴다(U001식
 * prefix 번호 체계는 옛 프로젝트에 있었지만 여기서는 승계하지 않았다 — 안드로이드와의
 * 명세 회의에서 prefix 복귀가 결정되면 이 enum 상수 값만 바꾸면 된다).
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "DUPLICATE_LOGIN_ID", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."),
    KAKAO_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "KAKAO_LOGIN_FAILED", "카카오 로그인에 실패했습니다."),
    KAKAO_APP_NOT_ALLOWED(HttpStatus.UNAUTHORIZED, "KAKAO_APP_NOT_ALLOWED", "허용되지 않은 카카오 앱의 토큰입니다."),
    KAKAO_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "KAKAO_EMAIL_REQUIRED", "카카오 계정의 이메일 제공 동의가 필요합니다."),
    KAKAO_EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "KAKAO_EMAIL_NOT_VERIFIED", "유효하고 인증된 카카오 이메일이 필요합니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
