package com.ssafy.modera.api.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImageSemanticSearchRequest(
        @Schema(description = "자연어 검색 문장 또는 단어", example = "C++ 프로그래밍 책 가격을 보여줘")
        @NotBlank
        @Size(max = 500)
        String query,

        @Schema(description = "페이지 번호", example = "0", defaultValue = "0")
        @Min(0)
        Integer page,

        @Schema(description = "페이지 크기", example = "20", defaultValue = "20")
        @Min(1)
        Integer size
) {
}
