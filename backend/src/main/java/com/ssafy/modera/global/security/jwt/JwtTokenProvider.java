package com.ssafy.modera.global.security.jwt;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public interface JwtTokenProvider {

    String createAccessToken(Long userId, String email, Collection<? extends GrantedAuthority> authorities);

    /**
     * 기기별 Refresh Token 을 발급한다.
     * 매 발급마다 고유한 jti 를 부여하므로 같은 사용자·같은 시각이라도 토큰(및 해시)이 절대 겹치지 않는다.
     */
    String createRefreshToken(Long userId, String email, String deviceId);
}
