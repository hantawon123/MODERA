package com.ssafy.modera.core.domain.calendar

import com.ssafy.modera.core.data.repository.calendar.CalendarRepository
import com.ssafy.modera.core.data.repository.calendar.DeviceCalendarRepository
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.model.calendar.toVisibleGridDateRange
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class GetCalendarSchedulesUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val deviceCalendarRepository: DeviceCalendarRepository,
) {

    fun appSchedulesForVisibleGrid(
        visibleMonth: YearMonth,
        selectedDate: LocalDate,
    ): Flow<List<CalendarSchedule>> {
        val gridRange = visibleMonth.toVisibleGridDateRange()
        val from = minOf(gridRange.start, selectedDate)
        val to = maxOf(gridRange.endInclusive, selectedDate)

        return calendarRepository.getSchedules(
            from = from,
            to = to,
        )
    }

    fun deviceScheduleCountsForVisibleGrid(
        visibleMonth: YearMonth,
    ): Flow<Map<LocalDate, Int>> {
        val range = visibleMonth.toVisibleGridDateRange()
        return deviceCalendarRepository.getScheduleCountsForRange(
            from = range.start,
            toInclusive = range.endInclusive,
        )
    }

    fun deviceSchedulesForDate(
        date: LocalDate,
    ): Flow<List<CalendarSchedule>> =
        deviceCalendarRepository.getSchedulesForDate(date)
}
