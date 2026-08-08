package com.ssafy.modera.core.data.repository.calendar

import com.ssafy.modera.core.model.calendar.CalendarSchedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CalendarRepository {

    fun getSchedules(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<CalendarSchedule>>

    fun getSchedulesByImageId(
        imageId: Long,
    ): Flow<List<CalendarSchedule>>

    fun registerSchedule(
        scheduleId: Long,
        imageId: Long,
    ): Flow<Unit>

    fun deleteSchedule(
        scheduleId: Long,
        imageId: Long,
    ): Flow<Unit>
}
