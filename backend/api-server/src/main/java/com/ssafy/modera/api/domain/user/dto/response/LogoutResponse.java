package com.ssafy.modera.api.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LogoutResponse(
        @Schema(description = "로그아웃 성공 여부", example = "true") Boolean loggedOut
) {

    public static LogoutResponse success() {
        return new LogoutResponse(true);
    }
}
