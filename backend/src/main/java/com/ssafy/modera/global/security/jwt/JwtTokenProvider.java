package com.ssafy.modera.global.security.jwt;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public interface JwtTokenProvider {

    String createAccessToken(Long userId, String email, Collection<? extends GrantedAuthority> authorities);

    String createRefreshToken(String email);
}
