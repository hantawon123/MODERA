package com.ssafy.modera.core.data

import app.cash.turbine.test
import com.ssafy.modera.core.data.repository.analyzedImage.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.calendar.CalendarRepository
import com.ssafy.modera.core.data.repository.calendar.DefaultCalendarRepository
import com.ssafy.modera.core.model.calendar.CalendarScheduleSource
import com.ssafy.modera.core.network.model.calendar.ScheduleResponse
import com.ssafy.modera.core.network.model.calendar.SchedulesRequest
import com.ssafy.modera.core.network.model.calendar.SchedulesResponse
import com.ssafy.modera.core.network.service.CalendarClient
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

class DefaultCalendarRepositoryTest {

    private lateinit var calendarClient: CalendarClient
    private lateinit var analyzedImageRepository: AnalyzedImageRepository
    private lateinit var repository: CalendarRepository

    private val testDispatcher = StandardTestDispatcher()
    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setUp() {
        calendarClient = mock()
        analyzedImageRepository = mock()

        repository = DefaultCalendarRepository(
            calendarClient = calendarClient,
            analyzedImageRepository = analyzedImageRepository,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun getSchedulesReturnsMappedSchedules() = runTest(testDispatcher) {
        val date = LocalDate.of(2026, 8, 9)
        val request = SchedulesRequest(
            from = date.atStartOfDay(zoneId).toInstant().toString(),
            to = date.plusDays(1).atStartOfDay(zoneId).toInstant().minusMillis(1).toString(),
        )
        val response = SchedulesResponse(
            list = listOf(
                ScheduleResponse(
                    scheduleId = 2L,
                    imageId = 10L,
                    title = "성심당 케이크 예약",
                    startAt = "2026-08-09T00:00:00.000Z",
                    endAt = "2026-08-09T04:00:00.000Z",
                    calendared = true,
                    updatedAt = "2026-08-01T00:00:00.000Z",
                ),
                ScheduleResponse(
                    scheduleId = 3L,
                    imageId = 12L,
                    title = "저녁 약속",
                    startAt = "2026-08-09T19:00:00+09:00",
                    endAt = "2026-08-09T21:00:00+09:00",
                    calendared = true,
                    updatedAt = "2026-08-01T00:00:00+09:00",
                ),
                ScheduleResponse(
                    scheduleId = 8L,
                    imageId = 11L,
                    title = "KTX 예매",
                    startAt = null,
                    endAt = null,
                    calendared = false,
                    updatedAt = "2026-08-01T00:00:00.000Z",
                ),
                ScheduleResponse(
                    scheduleId = 4L,
                    imageId = 67L,
                    title = "공모전 마감",
                    startAt = null,
                    endAt = "2026-08-09T18:00:00+09:00",
                    calendared = false,
                    updatedAt = "2026-08-03T09:07:01.187641+09:00",
                ),
            ),
            page = 0,
            size = 20,
            totalElements = 4,
            totalPages = 1,
            hasNext = false,
            hasPrevious = false,
        )

        whenever(calendarClient.fetchSchedules(request)).thenReturn(response)

        repository.getSchedules(from = date, to = date).test {
            val schedules = awaitItem()

            assertEquals(4, schedules.size)

            val addedSchedule = schedules[0]
            assertEquals(2L, addedSchedule.id)
            assertEquals(10L, addedSchedule.imageId)
            assertEquals("성심당 케이크 예약", addedSchedule.title)
            assertEquals(CalendarScheduleSource.APP, addedSchedule.source)
            assertEquals(
                Instant.parse("2026-08-09T00:00:00.000Z").atZone(zoneId).toLocalDate(),
                addedSchedule.date,
            )
            assertEquals(
                Instant.parse("2026-08-09T00:00:00.000Z").atZone(zoneId).toLocalTime(),
                addedSchedule.startTime,
            )
            assertEquals(
                Instant.parse("2026-08-09T04:00:00.000Z").atZone(zoneId).toLocalTime(),
                addedSchedule.endTime,
            )
            assertEquals(true, addedSchedule.isAdded)

            val offsetSchedule = schedules[1]
            assertEquals(3L, offsetSchedule.id)
            assertEquals(LocalDate.of(2026, 8, 9), offsetSchedule.date)
            assertEquals(
                OffsetDateTime.parse("2026-08-09T19:00:00+09:00")
                    .atZoneSameInstant(zoneId)
                    .toLocalTime(),
                offsetSchedule.startTime,
            )
            assertEquals(true, offsetSchedule.isAdded)

            val pendingSchedule = schedules[2]
            assertEquals(8L, pendingSchedule.id)
            assertEquals(null, pendingSchedule.date)
            assertEquals(null, pendingSchedule.startTime)
            assertEquals(null, pendingSchedule.endTime)
            assertEquals(false, pendingSchedule.isAdded)

            val endOnlySchedule = schedules[3]
            assertEquals(4L, endOnlySchedule.id)
            assertEquals(LocalDate.of(2026, 8, 9), endOnlySchedule.date)
            assertEquals(null, endOnlySchedule.startTime)
            assertEquals(
                OffsetDateTime.parse("2026-08-09T18:00:00+09:00")
                    .atZoneSameInstant(zoneId)
                    .toLocalTime(),
                endOnlySchedule.endTime,
            )
            assertEquals(false, endOnlySchedule.isAdded)

            awaitComplete()
        }

        verify(calendarClient).fetchSchedules(request)
    }
}
