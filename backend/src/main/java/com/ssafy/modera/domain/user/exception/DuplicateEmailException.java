package com.ssafy.modera.domain.user.exception;

import com.ssafy.modera.global.domain.ErrorCode;
import com.ssafy.modera.global.exception.BusinessException;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}
