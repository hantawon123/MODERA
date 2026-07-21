package com.ssafy.modera.global.security.filter;

import com.ssafy.modera.global.exception.AuthorizationException;
import com.ssafy.modera.global.security.jwt.JwtTokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenValidator jwtTokenValidator;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<String> excludeUrlPatterns = List.of(
            "/",
            "/favicon.ico",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/auth/login"
            // TODO 인증이 필요 없는 URL 패턴 추가
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return excludeUrlPatterns.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Optional<String> tokenOp = jwtTokenValidator.resolveToken(request);

        if (tokenOp.isPresent()) {
            String token = tokenOp.get();
            try {
                jwtTokenValidator.validateToken(token);

                Authentication authentication = jwtTokenValidator.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("Security Context 내에 인증 정보 저장: Name = {}, uri: {}", authentication.getName(), request.getRequestURI());
            } catch (AuthorizationException e) {
                request.setAttribute("exception", e);
                log.warn("JWT 인증 실패: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
