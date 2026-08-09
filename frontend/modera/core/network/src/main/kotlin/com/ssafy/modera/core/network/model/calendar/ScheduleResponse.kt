package com.ssafy.modera.core.network.model.calendar

import com.ssafy.modera.core.common.datetime.ModeraDateFormatter
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.model.calendar.CalendarScheduleSource
import kotlinx.serialization.Serializable
import java.time.ZoneId

@Serializable
data class ScheduleResponse(
    val scheduleId: Long,
    val imageId: Long,
    val title: String,
    val startAt: String? = null,
    val endAt: String? = null,
    val calendared: Boolean,
    val updatedAt: String,
)

fun ScheduleResponse.asExternalModel(
    zoneId: ZoneId = ZoneId.systemDefault(),
): CalendarSchedule {
    val start = ModeraDateFormatter.parseToZonedDateTimeOrNull(startAt, zoneId)
    val end = ModeraDateFormatter.parseToZonedDateTimeOrNull(endAt, zoneId)

    return CalendarSchedule(
        id = scheduleId,
        title = title,
        source = CalendarScheduleSource.APP,
        date = (start ?: end)?.toLocalDate(),
        startTime = start?.toLocalTime(),
        endTime = end?.toLocalTime(),
        isAdded = calendared,
        imageId = imageId,
    )
}
