package com.ssafy.modera.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답 (3-1)")
public record RegisterResponse(

        @Schema(description = "생성된 사용자 ID", example = "1")
        Long userId
) {

    public static RegisterResponse of(Long userId) {
        return new RegisterResponse(userId);
    }
}
