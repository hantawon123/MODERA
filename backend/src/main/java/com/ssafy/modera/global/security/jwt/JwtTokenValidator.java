package com.ssafy.modera.global.security.jwt;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public interface JwtTokenValidator {

    void validateToken(String token);

    Authentication getAuthentication(String token);

    Optional<String> resolveToken(HttpServletRequest request);

    Optional<String> getEmailFromRefreshToken(String token);
}
