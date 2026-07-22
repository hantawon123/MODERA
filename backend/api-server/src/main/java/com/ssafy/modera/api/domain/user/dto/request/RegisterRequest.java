package com.ssafy.modera.api.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 4, max = 20) String loginId,
        @NotBlank @Size(min = 8) String password,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 30) String nickname
) {
}
