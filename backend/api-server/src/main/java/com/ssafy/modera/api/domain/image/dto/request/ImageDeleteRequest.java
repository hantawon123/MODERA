package com.ssafy.modera.api.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ImageDeleteRequest(
        @Schema(description = "삭제할 이미지 ID 목록. 단건 삭제도 ID 하나를 배열로 전달한다.")
        @NotEmpty
        @Size(max = 100)
        List<@NotNull @Positive Integer> imageIds
) {
}
