package com.ssafy.modera.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그아웃 응답 (3-4)")
public record LogoutResponse(

        @Schema(description = "폐기 완료 여부", example = "true")
        boolean loggedOut
) {

    public static LogoutResponse success() {
        return new LogoutResponse(true);
    }
}
