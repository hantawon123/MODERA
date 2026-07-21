package com.ssafy.modera.global.docs.error.errors;

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
        description = "인증 실패 - JWT 토큰이 없거나 유효하지 않음",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                        name = "인증 실패 응답 예시",
                        value = """
                                {
                                    "result": "FAIL",
                                    "code": "A005",
                                    "message": "유효하지 않은 JWT 토큰입니다.",
                                    "timestamp": "2026-01-17 12:00:00"
                                }
                                """
                )
        )
)
public @interface CommonUnAuthorizedResponseDocs {
}
