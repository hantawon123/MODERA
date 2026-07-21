package com.ssafy.modera.global.domain.dto;

import org.springframework.data.domain.Slice;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record SliceResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        boolean hasPrevious,
        boolean hasNext,
        boolean first,
        boolean last
) {

    public static <T> SliceResponse<T> from(Slice<T> slice) {
        Objects.requireNonNull(slice, "Slice 객체는 null일 수 없습니다.");
        return new SliceResponse<>(
                slice.getContent(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasPrevious(),
                slice.hasNext(),
                slice.isFirst(),
                slice.isLast()
        );
    }

    public static <T> SliceResponse<T> empty() {
        return new SliceResponse<>(Collections.emptyList(), 0, 0, false, false, true, true);
    }
}
