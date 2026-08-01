package com.ssafy.modera.core.data.repository.calendar

import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.network.model.calendar.ScheduleResponse
import com.ssafy.modera.core.network.model.calendar.SchedulesRequest
import com.ssafy.modera.core.network.model.calendar.asExternalModel
import com.ssafy.modera.core.network.service.CalendarClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class DefaultCalendarRepository @Inject constructor(
    private val calendarClient: CalendarClient,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : CalendarRepository {

    private val zoneId = ZoneId.systemDefault()

    override fun getSchedules(
        from: LocalDate,
        to: LocalDate,
    ): Flow<List<CalendarSchedule>> = flow {
        emit(
            fetchAllSchedules(
                SchedulesRequest(
                    from = from.toApiFrom(zoneId),
                    to = to.toApiTo(zoneId),
                ),
            ).map { schedule ->
                schedule.asExternalModel(zoneId)
            },
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

    private fun LocalDate.toApiFrom(
        zoneId: ZoneId,
    ): String =
        atStartOfDay(zoneId)
            .toInstant()
            .toString()

    private fun LocalDate.toApiTo(
        zoneId: ZoneId,
    ): String =
        plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .minusMillis(1)
            .toString()
}
