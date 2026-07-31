package com.ssafy.modera.api.domain.schedule.service;

import com.ssafy.modera.api.domain.schedule.dto.response.ScheduleSummaryResponse;
import com.ssafy.modera.api.domain.schedule.repository.ScheduleListPage;
import com.ssafy.modera.api.domain.schedule.repository.ScheduleListRow;
import com.ssafy.modera.api.domain.schedule.repository.ScheduleQueryRepository;
import com.ssafy.modera.api.global.exception.BusinessException;
import com.ssafy.modera.api.global.exception.GlobalErrorCode;
import com.ssafy.modera.api.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleQueryService {

    private static final Set<String> SUPPORTED_SORTS = Set.of("START_ASC", "START_DESC");

    private final ScheduleQueryRepository scheduleQueryRepository;

    public PageResponse<ScheduleSummaryResponse> getSchedules(
            Integer userId,
            Boolean calendared,
            String from,
            String to,
            int page,
            int size,
            String sort
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(GlobalErrorCode.INVALID_PARAMETER);
        }

        String normalizedSort = sort == null || sort.isBlank()
                ? "START_ASC"
                : sort.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_SORTS.contains(normalizedSort)) {
            throw new BusinessException(GlobalErrorCode.INVALID_PARAMETER);
        }

        OffsetDateTime fromAt = parseDateTime(from);
        OffsetDateTime toAt = parseDateTime(to);
        if (fromAt != null && toAt != null && fromAt.isAfter(toAt)) {
            throw new BusinessException(GlobalErrorCode.INVALID_PARAMETER);
        }

        ScheduleListPage result = scheduleQueryRepository.findSchedules(
                userId, calendared, fromAt, toAt, normalizedSort, page, size);
        List<ScheduleSummaryResponse> list = result.content().stream()
                .map(this::toSummary)
                .toList();
        int totalPages = result.totalElements() == 0
                ? 0
                : (int) ((result.totalElements() + size - 1) / size);

        return new PageResponse<>(
                list,
                page,
                size,
                result.totalElements(),
                totalPages,
                page + 1 < totalPages,
                page > 0
        );
    }

    private ScheduleSummaryResponse toSummary(ScheduleListRow row) {
        return new ScheduleSummaryResponse(
                row.scheduleId(),
                row.imageId(),
                row.title(),
                row.startAt(),
                row.endAt(),
                row.calendared(),
                row.updatedAt()
        );
    }

    private OffsetDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new BusinessException(GlobalErrorCode.INVALID_PARAMETER);
        }
    }
}
