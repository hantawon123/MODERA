package com.ssafy.modera.domain.user.docs;

import com.ssafy.modera.domain.user.docs.error.LoginErrorDocs;
import com.ssafy.modera.domain.user.docs.error.RefreshTokenErrorDocs;
import com.ssafy.modera.domain.user.docs.error.RegisterErrorDocs;
import com.ssafy.modera.domain.user.dto.request.LoginRequest;
import com.ssafy.modera.domain.user.dto.request.LogoutRequest;
import com.ssafy.modera.domain.user.dto.request.RegisterRequest;
import com.ssafy.modera.domain.user.dto.request.ReissueRequest;
import com.ssafy.modera.domain.user.dto.response.LoginResponse;
import com.ssafy.modera.domain.user.dto.response.LogoutResponse;
import com.ssafy.modera.domain.user.dto.response.RegisterResponse;
import com.ssafy.modera.domain.user.dto.response.ReissueResponse;
import com.ssafy.modera.global.domain.dto.CommonResponse;
import com.ssafy.modera.global.security.principal.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 인증 API(3장) Swagger 문서. 구현은 AuthController 가 담당한다.
 */
@Tag(name = "01. Auth", description = "회원가입·로그인·토큰 재발급·로그아웃 API")
public interface AuthApiDocs {

    @Operation(
            summary = "[3-1] 회원가입",
            description = "새로운 계정을 등록한다. 비밀번호는 bcrypt 로 암호화해 저장한다."
    )
    @ApiResponse(responseCode = "201", description = "회원가입 성공")
    @SecurityRequirements
    @RegisterErrorDocs
    CommonResponse<RegisterResponse> register(RegisterRequest request);

    @Operation(
            summary = "[3-2] 로그인",
            description = """
                    loginId·password 를 검증하고 Access/Refresh Token 을 발급한다..

                    - refreshToken 은 쿠키가 아닌 응답 body 로 전달한다.
                    - deviceId 를 함께 보내면 기기 단위로 refreshToken 이 관리되어 3-4 로그아웃 시 해당 기기만 폐기된다.
                    - 존재하지 않는 ID 와 비밀번호 불일치는 구분하지 않는다,
                    """
    )
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @SecurityRequirements
    @LoginErrorDocs
    CommonResponse<LoginResponse> login(LoginRequest request);

    @Operation(
            summary = "[3-3] 토큰 재발급",
            description = """
                    refreshToken 으로 Access Token 을 재발급한다.

                    제시한 refreshToken 은 즉시 폐기되고 새 refreshToken 이 함께 발급된다(RTR).
                    """
    )
    @ApiResponse(responseCode = "200", description = "재발급 성공")
    @SecurityRequirements
    @RefreshTokenErrorDocs
    CommonResponse<ReissueResponse> reissue(ReissueRequest request);

    @Operation(
            summary = "[3-4] 로그아웃",
            description = "현재 기기(deviceId)의 refreshToken 을 폐기한다. Access Token 인증이 필요하다."
    )
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @RefreshTokenErrorDocs
    CommonResponse<LogoutResponse> logout(PrincipalDetails principal, LogoutRequest request);
}
