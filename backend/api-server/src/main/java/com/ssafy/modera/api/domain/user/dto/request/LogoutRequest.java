package com.ssafy.modera.api.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank String refreshToken,
        @NotBlank String deviceId
) {
}
