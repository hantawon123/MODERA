package com.ssafy.modera.api.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.api.global.exception.ErrorCode;
import com.ssafy.modera.api.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * AuthenticationEntryPoint/AccessDeniedHandler는 Spring MVC 밖(필터 체인)에서
 * 실행되므로 @RestControllerAdvice가 못 잡는다 — HttpServletResponse에 직접 써야 한다.
 */
final class SecurityErrorResponseWriter {

    private SecurityErrorResponseWriter() {
    }

    static void write(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Object> body = ApiResponse.fail(errorCode, errorCode.getMessage(), null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
