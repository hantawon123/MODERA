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
        responseCode = "409",
        description = "이미 사용 중인 로그인 ID 또는 이메일",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = {
                        @ExampleObject(
                                name = "DUPLICATE_LOGIN_ID",
                                value = """
                                        {
                                            "result": "FAIL",
                                            "code": "U007",
                                            "message": "이미 사용 중인 로그인 ID입니다.",
                                            "timestamp": "2026-01-17 12:00:00"
                                        }
                                        """
                        ),
                        @ExampleObject(
                                name = "DUPLICATE_EMAIL",
                                value = """
                                        {
                                            "result": "FAIL",
                                            "code": "U001",
                                            "message": "이미 사용 중인 이메일입니다.",
                                            "timestamp": "2026-01-17 12:00:00"
                                        }
                                        """
                        )
                }
        )
)
public @interface DuplicateAccountResponseDocs {
}
