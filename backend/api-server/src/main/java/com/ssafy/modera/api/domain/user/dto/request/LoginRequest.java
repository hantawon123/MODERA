package com.ssafy.modera.api.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String loginId,
        @NotBlank String password,
        @NotBlank String deviceId
) {
}
