package com.ssafy.modera.domain.user.docs.error.errors;

import com.ssafy.modera.global.domain.dto.CommonResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ApiResponse(
        responseCode = "401",
        description = "로그인 실패 - 존재하지 않는 ID 와 비밀번호 불일치를 구분하지 않는다(계정 열거 방지)",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                        name = "LOGIN_FAILED",
                        value = """
                                {
                                    "result": "FAIL",
                                    "code": "U008",
                                    "message": "아이디 또는 비밀번호가 올바르지 않습니다.",
                                    "timestamp": "2026-01-17 12:00:00"
                                }
                                """
                )
        )
)
public @interface LoginFailedResponseDocs {
}
