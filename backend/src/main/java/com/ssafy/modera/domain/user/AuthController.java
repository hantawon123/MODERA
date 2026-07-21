package com.ssafy.modera.domain.user;

import com.ssafy.modera.domain.user.dto.LoginRequest;
import com.ssafy.modera.domain.user.dto.LoginResponse;
import com.ssafy.modera.global.domain.dto.CommonResponse;
import com.ssafy.modera.global.security.jwt.TokenCookieFactory;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final TokenCookieFactory tokenCookieFactory;

    @PostMapping("/login")
    public CommonResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request);

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                tokenCookieFactory.createRefreshTokenCookie(result.refreshToken()).toString()
        );

        return CommonResponse.onSuccess(new LoginResponse(result.accessToken()));
    }
}