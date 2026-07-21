package com.ssafy.modera.domain.user.exception;

import com.ssafy.modera.global.domain.ErrorCode;
import com.ssafy.modera.global.exception.BusinessException;

/**
 * 로그인 실패. 존재하지 않는 loginId 와 비밀번호 불일치를 절대 구분하지 않는다.
 * (계정 열거 방지 — API 명세 3-2)
 */
public class LoginFailedException extends BusinessException {

    public LoginFailedException() {
        super(ErrorCode.LOGIN_FAILED);
    }
}
