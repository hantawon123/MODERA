package com.ssafy.modera.api.global.security.filter;

import com.ssafy.modera.api.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * @Component로 등록하지 않는다 — Spring Boot가 Filter 빈을 자동으로 모든 요청에
 * 걸어버려서(FilterRegistrationBean 자동구성) SecurityConfig의 addFilterBefore와
 * 중복 등록될 수 있다. SecurityConfig에서 new로 직접 만들어 체인에 끼워 넣는다.
 * <p>
 * Bearer 토큰이 유효하면 SecurityContext의 principal을 userId(Integer) 그 자체로
 * 설정한다 — 컨트롤러는 {@code @AuthenticationPrincipal Integer userId}로 바로 받는다.
 * /internal/**은 X-Webhook-Token으로 별도 인증하므로 이 필터를 아예 타지 않는다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        resolveToken(request)
                .filter(jwtTokenProvider::isValid)
                .ifPresent(token -> {
                    Integer userId = jwtTokenProvider.getUserId(token);
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });

        filterChain.doFilter(request, response);
    }

    private Optional<String> resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return Optional.of(bearerToken.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
