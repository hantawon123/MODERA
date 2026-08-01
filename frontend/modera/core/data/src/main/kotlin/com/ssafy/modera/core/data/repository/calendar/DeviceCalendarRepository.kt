package com.ssafy.modera.core.data.repository.calendar

import com.ssafy.modera.core.model.calendar.CalendarSchedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DeviceCalendarRepository {

    fun getSchedulesForDate(date: LocalDate): Flow<List<CalendarSchedule>>

    fun getScheduleCountsForRange(
        from: LocalDate,
        toInclusive: LocalDate,
    ): Flow<Map<LocalDate, Int>>
}
