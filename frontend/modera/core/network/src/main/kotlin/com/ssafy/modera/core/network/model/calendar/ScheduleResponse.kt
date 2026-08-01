package com.ssafy.modera.core.network.model.calendar

import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.model.calendar.CalendarScheduleSource
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalTime
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
): CalendarSchedule =
    CalendarSchedule(
        id = scheduleId,
        title = title,
        source = CalendarScheduleSource.APP,
        startTime = startAt?.toLocalTimeOrNull(zoneId),
        endTime = endAt?.toLocalTimeOrNull(zoneId),
        isAdded = calendared,
    )

fun ScheduleResponse.scheduleDate(
    zoneId: ZoneId = ZoneId.systemDefault(),
) = startAt?.let { Instant.parse(it).atZone(zoneId).toLocalDate() }

private fun String.toLocalTimeOrNull(
    zoneId: ZoneId,
): LocalTime? =
    runCatching {
        Instant.parse(this).atZone(zoneId).toLocalTime()
    }.getOrNull()
