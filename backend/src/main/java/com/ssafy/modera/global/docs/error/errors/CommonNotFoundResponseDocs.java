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
        responseCode = "404",
        description = "리소스를 찾을 수 없음",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CommonResponse.class),
                examples = @ExampleObject(
                        name = "리소스 없음 응답 예시",
                        value = """
                                {
                                    "result": "FAIL",
                                    "code": "G006",
                                    "message": "요청하신 리소스를 찾을 수 없습니다.",
                                    "timestamp": "2026-01-17 12:00:00"
                                }
                                """
                )
        )
)
public @interface CommonNotFoundResponseDocs {
}
