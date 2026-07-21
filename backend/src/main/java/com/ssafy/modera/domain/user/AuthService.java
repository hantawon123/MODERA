package com.ssafy.modera.domain.user;

import com.ssafy.modera.domain.user.dto.LoginRequest;
import com.ssafy.modera.global.domain.ErrorCode;
import com.ssafy.modera.global.exception.BusinessException;
import com.ssafy.modera.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.getRole()));

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getEmail(), authorities);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        return new LoginResult(accessToken, refreshToken);
    }

    public record LoginResult(String accessToken, String refreshToken) {
    }
}
