package com.ssafy.modera.global.security.jwt;

import com.ssafy.modera.global.domain.GlobalErrorCode;
import com.ssafy.modera.global.exception.AuthorizationException;
import com.ssafy.modera.global.security.dto.AuthUser;
import com.ssafy.modera.global.security.principal.PrincipalDetails;
import com.ssafy.modera.global.security.token.JwtAuthenticationToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenValidatorImpl implements JwtTokenValidator {
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORITIES_KEY = "auth";
    private static final String USER_ID_KEY = "uid";

    private final JwtProperties jwtProperties;
    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public void validateToken(String token) {
        try {
            parseClaims(token);
        } catch (SecurityException | MalformedJwtException e) {
            throw new AuthorizationException(GlobalErrorCode.JWT_MALFORMED);
        } catch (ExpiredJwtException e) {
            throw new AuthorizationException(GlobalErrorCode.JWT_EXPIRED);
        } catch (UnsupportedJwtException e) {
            throw new AuthorizationException(GlobalErrorCode.JWT_INVALID);
        } catch (IllegalArgumentException e) {
            throw new AuthorizationException(GlobalErrorCode.JWT_MISSING);
        }
    }

    @Override
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        if (claims.get(AUTHORITIES_KEY) == null || claims.get(USER_ID_KEY) == null) {
            throw new AuthorizationException(GlobalErrorCode.UNAUTHORIZED);
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        Long userId = claims.get(USER_ID_KEY, Long.class);
        String email = claims.getSubject();

        AuthUser detachedUser = AuthUser.of(userId, email, authorities.iterator().next().getAuthority());

        PrincipalDetails principal = PrincipalDetails.from(detachedUser);

        return new JwtAuthenticationToken(principal, token, authorities);
    }

    @Override
    public Optional<String> resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_AUTHORIZATION);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return Optional.of(bearerToken.substring(7));
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> getEmailFromRefreshToken(String token) {
        Claims claims = parseClaims(token);

        if (claims.getSubject() == null) {
            throw new AuthorizationException(GlobalErrorCode.UNAUTHORIZED);
        }

        return Optional.ofNullable(claims.getSubject());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
