package com.ssafy.modera.api.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(description = "로그인 아이디, 4~20자", example = "moderauser")
        @NotBlank @Size(min = 4, max = 20) String loginId,

        @Schema(description = "비밀번호, 8자 이상. bcrypt로 해싱해 저장한다", example = "password123")
        @NotBlank @Size(min = 8) String password,

        @Schema(description = "이메일", example = "user@example.com")
        @NotBlank @Email @Size(max = 255) String email,

        @Schema(
                description = "하위 호환용 필드이며 현재 저장하지 않습니다.",
                example = "모데라",
                deprecated = true
        )
        @Size(max = 30) String nickname
) {
}
