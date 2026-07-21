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
        description = "유효하지 않거나 이미 폐기(회전)된 refreshToken, 또는 Access Token 인증 실패",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = {
                        @ExampleObject(
                                name = "INVALID_REFRESH_TOKEN",
                                value = """
                                        {
                                            "result": "FAIL",
                                            "code": "U009",
                                            "message": "유효하지 않은 리프레시 토큰입니다.",
                                            "timestamp": "2026-01-17 12:00:00"
                                        }
                                        """
                        ),
                        @ExampleObject(
                                name = "UNAUTHORIZED",
                                value = """
                                        {
                                            "result": "FAIL",
                                            "code": "A005",
                                            "message": "유효하지 않은 JWT 토큰입니다.",
                                            "timestamp": "2026-01-17 12:00:00"
                                        }
                                        """
                        )
                }
        )
)
public @interface InvalidRefreshTokenResponseDocs {
}
