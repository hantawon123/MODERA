package com.ssafy.modera.core.data

import app.cash.turbine.test
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
import java.time.YearMonth
import java.time.ZoneId

class DefaultCalendarRepositoryTest {

    private lateinit var calendarClient: CalendarClient
    private lateinit var repository: CalendarRepository

    private val testDispatcher = StandardTestDispatcher()
    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setUp() {
        calendarClient = mock()

        repository = DefaultCalendarRepository(
            calendarClient = calendarClient,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun getSchedulesForDateReturnsMappedSchedules() = runTest(testDispatcher) {
        val date = LocalDate.of(2026, 8, 9)
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
                    scheduleId = 8L,
                    imageId = 11L,
                    title = "KTX 예매",
                    startAt = null,
                    endAt = null,
                    calendared = false,
                    updatedAt = "2026-08-01T00:00:00.000Z",
                ),
            ),
            page = 0,
            size = 20,
            totalElements = 2,
            totalPages = 1,
            hasNext = false,
            hasPrevious = false,
        )

        whenever(
            calendarClient.fetchSchedules(
                SchedulesRequest(
                    from = "2026-08-09",
                    to = "2026-08-09",
                ),
            ),
        ).thenReturn(response)

        repository.getSchedulesForDate(date).test {
            val schedules = awaitItem()

            assertEquals(2, schedules.size)

            val addedSchedule = schedules.first()
            assertEquals(2L, addedSchedule.id)
            assertEquals("성심당 케이크 예약", addedSchedule.title)
            assertEquals(CalendarScheduleSource.APP, addedSchedule.source)
            assertEquals(
                Instant.parse("2026-08-09T00:00:00.000Z").atZone(zoneId).toLocalTime(),
                addedSchedule.startTime,
            )
            assertEquals(
                Instant.parse("2026-08-09T04:00:00.000Z").atZone(zoneId).toLocalTime(),
                addedSchedule.endTime,
            )
            assertEquals(true, addedSchedule.isAdded)

            val pendingSchedule = schedules.last()
            assertEquals(8L, pendingSchedule.id)
            assertEquals(null, pendingSchedule.startTime)
            assertEquals(null, pendingSchedule.endTime)
            assertEquals(false, pendingSchedule.isAdded)

            awaitComplete()
        }

        verify(calendarClient).fetchSchedules(
            SchedulesRequest(
                from = "2026-08-09",
                to = "2026-08-09",
            ),
        )
    }

    @Test
    fun getScheduleCountsForMonthReturnsCountsByDate() = runTest(testDispatcher) {
        val yearMonth = YearMonth.of(2026, 8)
        val response = SchedulesResponse(
            list = listOf(
                ScheduleResponse(
                    scheduleId = 2L,
                    imageId = 10L,
                    title = "성심당 케이크 예약",
                    startAt = "2026-08-09T00:00:00.000Z",
                    endAt = null,
                    calendared = true,
                    updatedAt = "2026-08-01T00:00:00.000Z",
                ),
                ScheduleResponse(
                    scheduleId = 5L,
                    imageId = 12L,
                    title = "KTX 예매",
                    startAt = "2026-08-11T03:00:00.000Z",
                    endAt = null,
                    calendared = false,
                    updatedAt = "2026-08-01T00:00:00.000Z",
                ),
                ScheduleResponse(
                    scheduleId = 6L,
                    imageId = 13L,
                    title = "카페 예약",
                    startAt = "2026-08-11T06:00:00.000Z",
                    endAt = null,
                    calendared = false,
                    updatedAt = "2026-08-01T00:00:00.000Z",
                ),
            ),
            page = 0,
            size = 20,
            totalElements = 3,
            totalPages = 1,
            hasNext = false,
            hasPrevious = false,
        )

        whenever(
            calendarClient.fetchSchedules(
                SchedulesRequest(
                    from = "2026-08-01",
                    to = "2026-08-31",
                ),
            ),
        ).thenReturn(response)

        repository.getScheduleCountsForMonth(yearMonth).test {
            val counts = awaitItem()

            assertEquals(1, counts[Instant.parse("2026-08-09T00:00:00.000Z").atZone(zoneId).toLocalDate()])
            assertEquals(2, counts[Instant.parse("2026-08-11T03:00:00.000Z").atZone(zoneId).toLocalDate()])

            awaitComplete()
        }

        verify(calendarClient).fetchSchedules(
            SchedulesRequest(
                from = "2026-08-01",
                to = "2026-08-31",
            ),
        )
    }
}
