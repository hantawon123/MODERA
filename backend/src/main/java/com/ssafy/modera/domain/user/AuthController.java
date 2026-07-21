package com.ssafy.modera.domain.user;

import com.ssafy.modera.domain.user.docs.AuthApiDocs;
import com.ssafy.modera.domain.user.dto.request.LoginRequest;
import com.ssafy.modera.domain.user.dto.request.LogoutRequest;
import com.ssafy.modera.domain.user.dto.request.RegisterRequest;
import com.ssafy.modera.domain.user.dto.request.ReissueRequest;
import com.ssafy.modera.domain.user.dto.response.LoginResponse;
import com.ssafy.modera.domain.user.dto.response.LogoutResponse;
import com.ssafy.modera.domain.user.dto.response.RegisterResponse;
import com.ssafy.modera.domain.user.dto.response.ReissueResponse;
import com.ssafy.modera.domain.user.service.AuthService;
import com.ssafy.modera.global.domain.dto.CommonResponse;
import com.ssafy.modera.global.security.principal.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApiDocs {

    private final AuthService authService;

    @Override
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return CommonResponse.onSuccess(authService.register(request));
    }

    @Override
    @PostMapping("/login")
    public CommonResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return CommonResponse.onSuccess(authService.login(request));
    }

    @Override
    @PostMapping("/refresh")
    public CommonResponse<ReissueResponse> reissue(@Valid @RequestBody ReissueRequest request) {
        return CommonResponse.onSuccess(authService.reissue(request));
    }

    @Override
    @PostMapping("/logout")
    public CommonResponse<LogoutResponse> logout(
            @AuthenticationPrincipal PrincipalDetails principal,
            @Valid @RequestBody LogoutRequest request
    ) {
        return CommonResponse.onSuccess(authService.logout(principal.getUserId(), request));
    }
}
