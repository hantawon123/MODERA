package com.ssafy.modera.api.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * X-Request-Id 헤더가 있으면 그대로, 없으면 UUID 앞 8자리를 생성해 MDC에 심고
 * 응답 헤더로도 돌려준다. 가장 먼저 실행되어야 Security가 막은 401/403 응답에도
 * requestId가 붙는다(@Order(HIGHEST_PRECEDENCE) — Spring Security 필터 체인 전체를
 * 감싼다). 액세스 로그(method path status 소요ms)도 여기서 함께 남긴다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String MDC_KEY_REQUEST_ID = "requestId";
    private static final String ACTUATOR_PATH_PREFIX = "/actuator";
    private static final int GENERATED_ID_LENGTH = 8;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put(MDC_KEY_REQUEST_ID, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            logAccess(request, response, System.currentTimeMillis() - startedAt);
            MDC.remove(MDC_KEY_REQUEST_ID);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String headerValue = request.getHeader(HEADER_REQUEST_ID);
        if (StringUtils.hasText(headerValue)) {
            return headerValue;
        }
        return UUID.randomUUID().toString().substring(0, GENERATED_ID_LENGTH);
    }

    private void logAccess(HttpServletRequest request, HttpServletResponse response, long elapsedMs) {
        if (request.getRequestURI().startsWith(ACTUATOR_PATH_PREFIX)) {
            return;
        }
        log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
    }
}
