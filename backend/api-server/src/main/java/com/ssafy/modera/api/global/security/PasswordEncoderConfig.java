package com.ssafy.modera.api.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * DelegatingPasswordEncoder를 쓰면 저장값에 {bcrypt} 접두어가 붙는다
 * (users.password_hash를 60자가 아니라 72자로 잡아둔 이유 — 접두어 포함해도 들어간다).
 * 나중에 인코딩 방식을 바꿔야 해도 접두어로 구분되니 기존 값과 공존할 수 있다.
 */
@Configuration
public class PasswordEncoderConfig {

    private static final String ENCODING_ID = "bcrypt";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder(ENCODING_ID, Map.of(ENCODING_ID, new BCryptPasswordEncoder(12)));
    }
}
