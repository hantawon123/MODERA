package com.ssafy.modera.api.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.api.global.exception.GlobalErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 인증은 됐지만 권한이 부족한 요청 → 403 + ApiResponse.fail envelope. */
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        SecurityErrorResponseWriter.write(response, objectMapper, GlobalErrorCode.FORBIDDEN);
    }
}
