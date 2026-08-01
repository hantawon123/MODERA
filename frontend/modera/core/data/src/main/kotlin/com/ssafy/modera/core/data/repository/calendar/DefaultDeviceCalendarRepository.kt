package com.ssafy.modera.core.data.repository.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.ssafy.modera.core.common.network.Dispatcher
import com.ssafy.modera.core.common.network.ModeraDispatcher
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.model.calendar.CalendarScheduleSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class DefaultDeviceCalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(ModeraDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : DeviceCalendarRepository {

    override fun getSchedulesForDate(date: LocalDate): Flow<List<CalendarSchedule>> =
        flow {
            emit(querySchedules(fromDate = date, toDateExclusive = date.plusDays(1)))
        }.flowOn(ioDispatcher)

    override fun getScheduleCountsForRange(
        from: LocalDate,
        toInclusive: LocalDate,
    ): Flow<Map<LocalDate, Int>> =
        flow {
            val zone = ZoneId.systemDefault()
            emit(
                queryScheduleCounts(
                    fromDate = from,
                    toDateExclusive = toInclusive.plusDays(1),
                    zone = zone,
                ),
            )
        }.flowOn(ioDispatcher)

    private fun queryScheduleCounts(
        fromDate: LocalDate,
        toDateExclusive: LocalDate,
        zone: ZoneId,
    ): Map<LocalDate, Int> {
        if (!hasReadCalendarPermission()) return emptyMap()

        val beginMillis = fromDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = toDateExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
        val counts = mutableMapOf<LocalDate, Int>()

        queryInstanceCursor(beginMillis = beginMillis, endMillis = endMillis)?.use { cursor ->
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            while (cursor.moveToNext()) {
                val begin = cursor.getLong(beginIndex)
                val end = cursor.getLong(endIndex)
                val allDay = cursor.getInt(allDayIndex) == 1

                val eventStartDate = if (allDay) {
                    Instant.ofEpochMilli(begin).atZone(ZoneId.of("UTC")).toLocalDate()
                } else {
                    Instant.ofEpochMilli(begin).atZone(zone).toLocalDate()
                }
                val eventEndDateExclusive = if (allDay) {
                    Instant.ofEpochMilli(end).atZone(ZoneId.of("UTC")).toLocalDate()
                } else {
                    val endDateTime = Instant.ofEpochMilli(end).atZone(zone)
                    if (endDateTime.toLocalTime() == LocalTime.MIDNIGHT) {
                        endDateTime.toLocalDate()
                    } else {
                        endDateTime.toLocalDate().plusDays(1)
                    }
                }

                var day = maxOf(eventStartDate, fromDate)
                val lastExclusive = minOf(eventEndDateExclusive, toDateExclusive)
                while (day.isBefore(lastExclusive)) {
                    counts[day] = (counts[day] ?: 0) + 1
                    day = day.plusDays(1)
                }
            }
        }

        return counts
    }

    private fun querySchedules(
        fromDate: LocalDate,
        toDateExclusive: LocalDate,
    ): List<CalendarSchedule> {
        if (!hasReadCalendarPermission()) return emptyList()

        val zone = ZoneId.systemDefault()
        val beginMillis = fromDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = toDateExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
        val schedules = mutableListOf<CalendarSchedule>()

        queryInstanceCursor(beginMillis = beginMillis, endMillis = endMillis)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            while (cursor.moveToNext()) {
                val eventId = cursor.getLong(idIndex)
                val title = cursor.getString(titleIndex)?.takeIf { it.isNotBlank() } ?: "제목 없음"
                val begin = cursor.getLong(beginIndex)
                val end = cursor.getLong(endIndex)
                val allDay = cursor.getInt(allDayIndex) == 1

                val (startTime, endTime) = if (allDay) {
                    null to null
                } else {
                    Instant.ofEpochMilli(begin).atZone(zone).toLocalTime() to
                        Instant.ofEpochMilli(end).atZone(zone).toLocalTime()
                }

                schedules += CalendarSchedule(
                    id = eventId,
                    title = title,
                    source = CalendarScheduleSource.DEVICE,
                    startTime = startTime,
                    endTime = endTime,
                    isAdded = false,
                )
            }
        }

        return schedules
    }

    private fun queryInstanceCursor(
        beginMillis: Long,
        endMillis: Long,
    ) = try {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let { builder ->
            ContentUris.appendId(builder, beginMillis)
            ContentUris.appendId(builder, endMillis)
            builder.build()
        }

        context.contentResolver.query(
            uri,
            INSTANCE_PROJECTION,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )
    } catch (_: SecurityException) {
        null
    }

    private fun hasReadCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        val INSTANCE_PROJECTION = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        )
    }
}
