package com.ssafy.modera.api.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modera.api.global.exception.GlobalErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 인증 자체가 안 된 요청(토큰 없음/무효) → 401 + ApiResponse.fail envelope. */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        SecurityErrorResponseWriter.write(response, objectMapper, GlobalErrorCode.UNAUTHORIZED);
    }
}
