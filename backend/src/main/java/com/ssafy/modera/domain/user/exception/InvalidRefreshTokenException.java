package com.ssafy.modera.domain.user.exception;

import com.ssafy.modera.global.domain.ErrorCode;
import com.ssafy.modera.global.exception.BusinessException;

/**
 * 서명·만료 검증 실패, 저장소에 없음, 이미 회전되어 폐기됨을 모두 동일하게 취급한다.
 */
public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
