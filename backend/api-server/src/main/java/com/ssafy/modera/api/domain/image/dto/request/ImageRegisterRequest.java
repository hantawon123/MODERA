package com.ssafy.modera.api.domain.image.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ImageRegisterRequest(
        @Schema(description = "등록할 이미지 목록. 각 항목은 독립적으로 처리된다.")
        @NotEmpty List<@NotNull @Valid ImageRegisterItemRequest> images
) {
}
