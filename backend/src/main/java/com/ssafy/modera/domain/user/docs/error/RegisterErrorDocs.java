package com.ssafy.modera.domain.user.docs.error;

import com.ssafy.modera.domain.user.docs.error.errors.DuplicateAccountResponseDocs;
import com.ssafy.modera.global.docs.error.errors.CommonBadRequestResponseDocs;
import com.ssafy.modera.global.docs.error.errors.CommonInternalServerErrorResponseDocs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@CommonBadRequestResponseDocs
@DuplicateAccountResponseDocs
@CommonInternalServerErrorResponseDocs
public @interface RegisterErrorDocs {
}
