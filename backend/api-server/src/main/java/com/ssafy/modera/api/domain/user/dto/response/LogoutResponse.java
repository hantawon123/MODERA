package com.ssafy.modera.api.domain.user.dto.response;

public record LogoutResponse(Boolean loggedOut) {

    public static LogoutResponse success() {
        return new LogoutResponse(true);
    }
}
