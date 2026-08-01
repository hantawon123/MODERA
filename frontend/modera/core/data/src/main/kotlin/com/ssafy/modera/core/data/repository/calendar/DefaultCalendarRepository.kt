package com.ssafy.modera.core.data.repository.calendar

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.network.model.calendar.ScheduleResponse
import com.ssafy.modera.core.network.model.calendar.SchedulesRequest
import com.ssafy.modera.core.network.model.calendar.asExternalModel
import com.ssafy.modera.core.network.model.calendar.scheduleDate
import com.ssafy.modera.core.network.service.CalendarClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class DefaultCalendarRepository @Inject constructor(
    private val calendarClient: CalendarClient,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : CalendarRepository {

    private val zoneId = ZoneId.systemDefault()

    override fun getSchedulesForDate(date: LocalDate): Flow<List<CalendarSchedule>> =
        flow {
            val schedules = fetchAllSchedules(
                SchedulesRequest(
                    from = date.toApiDateString(),
                    to = date.toApiDateString(),
                ),
            )

            emit(schedules.map { it.asExternalModel(zoneId) })
        }.flowOn(ioDispatcher)

    override fun getScheduleCountsForMonth(yearMonth: YearMonth): Flow<Map<LocalDate, Int>> =
        flow {
            val fromDate = yearMonth.atDay(1)
            val toDate = yearMonth.atEndOfMonth()
            val schedules = fetchAllSchedules(
                SchedulesRequest(
                    from = fromDate.toApiDateString(),
                    to = toDate.toApiDateString(),
                ),
            )

            emit(
                schedules
                    .mapNotNull { it.scheduleDate(zoneId) }
                    .groupingBy { it }
                    .eachCount(),
            )
        }.flowOn(ioDispatcher)

    private suspend fun fetchAllSchedules(
        request: SchedulesRequest,
    ): List<ScheduleResponse> {
        val schedules = mutableListOf<ScheduleResponse>()
        var page = request.page

        while (true) {
            val response = calendarClient.fetchSchedules(
                request.copy(page = page),
            )

            schedules += response.list

            if (!response.hasNext) {
                break
            }

            page += 1
        }

        return schedules
    }

    private fun LocalDate.toApiDateString(): String =
        format(DateTimeFormatter.ISO_LOCAL_DATE)
}
