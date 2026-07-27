package com.ssafy.modera.api.global.config;

import com.ssafy.modera.api.global.security.filter.JwtAuthenticationFilter;
import com.ssafy.modera.api.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    // /internal/** 은 컨트롤러 자체에서 X-Webhook-Token을 검증하고, JwtAuthenticationFilter도
    // 아예 타지 않는다(외부=JWT / 내부=공유 토큰 경계, backend-conventions.md 6절 참고).
    // register/login/refresh는 토큰이 아직 없는 시점에 호출하는 API라 permitAll이다.
    // logout은 Bearer가 필수라 여기 넣지 않는다(컨트롤러가 @AuthenticationPrincipal로 받음).
    private static final String[] PERMIT_ALL_PATHS = {
            "/actuator/health",
            "/actuator/health/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/internal/**",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/kakao/login",
            "/api/v1/auth/refresh"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
