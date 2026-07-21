package com.ssafy.modera.global.domain.dto;

import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        Objects.requireNonNull(page, "Page 객체는 null일 수 없습니다.");
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(Collections.emptyList(), 0, 0, 0, 1, true, true);
    }
}
