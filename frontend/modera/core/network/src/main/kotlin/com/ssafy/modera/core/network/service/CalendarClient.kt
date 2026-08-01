package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.getOrThrow
import com.ssafy.modera.core.network.model.calendar.SchedulesRequest
import com.ssafy.modera.core.network.model.calendar.SchedulesResponse
import com.ssafy.modera.core.network.model.calendar.UpdateScheduleCalendarRequest
import com.ssafy.modera.core.network.model.calendar.UpdateScheduleCalendarResponse
import javax.inject.Inject

class CalendarClient @Inject constructor(
    private val calendarService: CalendarService,
) {
    suspend fun fetchSchedules(
        request: SchedulesRequest = SchedulesRequest(),
    ): SchedulesResponse =
        calendarService
            .fetchSchedules(
                calendared = request.calendared,
                from = request.from,
                to = request.to,
                page = request.page,
                size = request.size,
                sort = request.sort.queryValue,
            )
            .getOrThrow()
            .data

    suspend fun registerSchedule(
        scheduleId: Long,
    ): UpdateScheduleCalendarResponse =
        calendarService
            .updateScheduleCalendarRegistration(
                scheduleId = scheduleId,
                request = UpdateScheduleCalendarRequest(calendared = true),
            )
            .getOrThrow()
            .data
}
