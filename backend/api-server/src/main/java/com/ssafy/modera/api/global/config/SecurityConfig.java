package com.ssafy.modera.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // /internal/** 은 컨트롤러 자체에서 X-Webhook-Token을 검증하므로 permitAll이다.
    // /api/v1/images/**는 JWT 인증이 아직 없어 임시로 열어뒀다(X-User-Id 헤더로 대체 중).
    // TODO: JWT 인증 필터 도입 후 /api/v1/images/**를 이 목록에서 제거할 것.
    private static final String[] PERMIT_ALL_PATHS = {
            "/actuator/health",
            "/actuator/health/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/internal/**",
            "/api/v1/images/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL_PATHS).permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
