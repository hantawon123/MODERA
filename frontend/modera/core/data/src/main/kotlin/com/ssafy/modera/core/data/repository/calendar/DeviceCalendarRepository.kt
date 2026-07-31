package com.ssafy.modera.core.data.repository.calendar

import com.ssafy.modera.core.model.calendar.CalendarSchedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface DeviceCalendarRepository {

    fun getSchedulesForDate(date: LocalDate): Flow<List<CalendarSchedule>>

    fun getScheduleCountsForMonth(yearMonth: YearMonth): Flow<Map<LocalDate, Int>>
}
