package com.ssafy.modera.global.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProviderImpl implements JwtTokenProvider {
    private static final String AUTHORITIES_KEY = "auth";
    private static final String USER_ID_KEY = "uid";
    private static final String DEVICE_ID_KEY = "did";

    private final JwtProperties jwtProperties;
    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String createAccessToken(Long userId, String email, Collection<? extends GrantedAuthority> authorities) {
        log.info("Access Token 생성 요청: userId={}, email={}", userId, email);

        Instant now = Instant.now();
        Instant accessTokenExpiresIn = now.plus(jwtProperties.accessTokenValidityInSeconds(), ChronoUnit.SECONDS);

        String authoritiesString = String.join(",", authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList());

        log.info("Access Token 생성 완료: email={}, accessExp={}", email, accessTokenExpiresIn);

        return Jwts.builder()
                .subject(email)
                .claim(USER_ID_KEY, userId)
                .claim(AUTHORITIES_KEY, authoritiesString)
                .issuedAt(Date.from(now))
                .expiration(Date.from(accessTokenExpiresIn))
                .signWith(key)
                .compact();
    }

    @Override
    public String createRefreshToken(Long userId, String email, String deviceId) {
        log.info("Refresh Token 생성 요청: userId={}, deviceId={}", userId, deviceId);

        Instant now = Instant.now();
        Instant refreshTokenExpiresIn = now.plus(jwtProperties.refreshTokenValidityInSeconds(), ChronoUnit.SECONDS);

        log.info("Refresh Token 생성 완료: userId={}, RefreshExp={}", userId, refreshTokenExpiresIn);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim(USER_ID_KEY, userId)
                .claim(DEVICE_ID_KEY, deviceId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(refreshTokenExpiresIn))
                .signWith(key)
                .compact();
    }
}
