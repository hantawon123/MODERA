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
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                        name = "서버 오류 응답 예시",
                        value = """
                                {
                                    "result": "FAIL",
                                    "code": "G001",
                                    "message": "예상치 못한 서버 오류입니다. 관리자에게 문의해주세요.",
                                    "timestamp": "2026-01-17 12:00:00"
                                }
                                """
                )
        )
)
public @interface CommonInternalServerErrorResponseDocs {
}
