package com.ssafy.modera.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청 (3-1)")
public record RegisterRequest(

        @Schema(description = "로그인 ID, 중복 불가", example = "newUser123")
        @NotBlank(message = "로그인 ID는 필수입니다.")
        @Size(min = 4, max = 20, message = "로그인 ID는 4~20자여야 합니다.")
        String loginId,

        @Schema(description = "비밀번호(8자 이상), 서버에서 bcrypt 저장", example = "securePassword123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @Schema(description = "이메일, 중복 불가", example = "user@example.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255, message = "이메일은 255자를 넘을 수 없습니다.")
        String email,

        @Schema(description = "표시 이름", example = "상현")
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 30, message = "닉네임은 30자를 넘을 수 없습니다.")
        String nickname
) {
}
