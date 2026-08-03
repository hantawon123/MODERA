package com.ssafy.modera.core.network.model.calendar

import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.model.calendar.CalendarScheduleSource
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

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
    val start = startAt.toZonedDateTimeOrNull(zoneId)
    val end = endAt.toZonedDateTimeOrNull(zoneId)

    return CalendarSchedule(
        id = scheduleId,
        title = title,
        source = CalendarScheduleSource.APP,
        date = (start ?: end)?.toLocalDate(),
        startTime = start?.toLocalTime(),
        endTime = end?.toLocalTime(),
        isAdded = calendared,
    )
}

private fun String?.toZonedDateTimeOrNull(zoneId: ZoneId): ZonedDateTime? {
    if (isNullOrBlank()) return null

    return runCatching { Instant.parse(this).atZone(zoneId) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(this).atZoneSameInstant(zoneId) }.getOrNull()
        ?: runCatching { ZonedDateTime.parse(this).withZoneSameInstant(zoneId) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(this).atZone(zoneId) }.getOrNull()
        ?: runCatching { LocalDate.parse(this).atStartOfDay(zoneId) }.getOrNull()
}
