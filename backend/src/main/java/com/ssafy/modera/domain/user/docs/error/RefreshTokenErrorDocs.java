package com.ssafy.modera.domain.user.docs.error;

import com.ssafy.modera.domain.user.docs.error.errors.InvalidRefreshTokenResponseDocs;
import com.ssafy.modera.global.docs.error.errors.CommonBadRequestResponseDocs;
import com.ssafy.modera.global.docs.error.errors.CommonInternalServerErrorResponseDocs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 3-3 토큰 재발급 / 3-4 로그아웃 공통 에러 문서....
 */
@Inherited
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@CommonBadRequestResponseDocs
@InvalidRefreshTokenResponseDocs
@CommonInternalServerErrorResponseDocs
public @interface RefreshTokenErrorDocs {
}
