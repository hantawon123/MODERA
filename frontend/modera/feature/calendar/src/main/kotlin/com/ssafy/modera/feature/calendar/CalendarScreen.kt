package com.ssafy.modera.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.model.calendar.CalendarScheduleSource
import com.ssafy.modera.feature.calendar.component.CalendarGrid
import com.ssafy.modera.feature.calendar.component.CalendarMonthHeader
import com.ssafy.modera.feature.calendar.component.CalendarScheduleSection
import com.ssafy.modera.feature.calendar.component.CalendarYearPickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@Composable
fun CalendarRoute(
    onBackClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onScheduleClick: (CalendarSchedule) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today) }
    var showYearPicker by remember { mutableStateOf(false) }
    val scheduleCountByDate = remember { CalendarDummyData.scheduleCountByDate }
    var schedulesByDate by remember {
        mutableStateOf(CalendarDummyData.schedulesByDate)
    }
    val schedulesForSelectedDate = schedulesByDate[selectedDate].orEmpty()

    CalendarScreen(
        visibleMonth = visibleMonth,
        selectedDate = selectedDate,
        today = today,
        scheduleCountByDate = scheduleCountByDate,
        schedules = schedulesForSelectedDate,
        showYearPicker = showYearPicker,
        onBackClick = onBackClick,
        onYearClick = { showYearPicker = true },
        onYearPickerDismiss = { showYearPicker = false },
        onYearSelect = { year ->
            visibleMonth = visibleMonth.withYear(year)
            selectedDate = selectedDate.withYear(year)
            showYearPicker = false
        },
        onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
        onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
        onDateClick = { date ->
            selectedDate = date
            visibleMonth = YearMonth.from(date)
        },
        onEditClick = onEditClick,
        onScheduleClick = onScheduleClick,
        onAddScheduleClick = { schedule ->
            schedulesByDate = schedulesByDate.mapValues { (date, schedules) ->
                if (date != selectedDate) {
                    schedules
                } else {
                    schedules.map { item ->
                        if (item.id == schedule.id) {
                            item.copy(isAdded = true)
                        } else {
                            item
                        }
                    }
                }
            }
        },
        modifier = modifier,
    )
}

@Composable
fun CalendarScreen(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    scheduleCountByDate: Map<LocalDate, Int>,
    schedules: List<CalendarSchedule>,
    showYearPicker: Boolean,
    onBackClick: () -> Unit,
    onYearClick: () -> Unit,
    onYearPickerDismiss: () -> Unit,
    onYearSelect: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onEditClick: () -> Unit,
    onScheduleClick: (CalendarSchedule) -> Unit,
    onAddScheduleClick: (CalendarSchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = ModeraTheme.colors.white),
    ) {
        ModeraTopBar(
            onBackClick = onBackClick,
            centerContent = {
                Text(
                    text = visibleMonth.year.toString(),
                    style = ModeraTheme.typography.bodySB16,
                    color = ModeraTheme.colors.gray900,
                    modifier = Modifier.clickable(onClick = onYearClick),
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            CalendarMonthHeader(
                visibleMonth = visibleMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
            )

            CalendarGrid(
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                today = today,
                scheduleCountByDate = scheduleCountByDate,
                onDateClick = onDateClick,
            )

            Spacer(modifier = Modifier.height(10.dp))

            CalendarScheduleSection(
                selectedDate = selectedDate,
                schedules = schedules,
                onEditClick = onEditClick,
                onScheduleClick = onScheduleClick,
                onAddScheduleClick = onAddScheduleClick,
            )
        }
    }

    if (showYearPicker) {
        CalendarYearPickerDialog(
            selectedYear = visibleMonth.year,
            onYearSelect = onYearSelect,
            onDismissRequest = onYearPickerDismiss,
        )
    }
}

internal object CalendarDummyData {
    private val augustNinth = LocalDate.of(2026, 8, 9)
    private val augustEleventh = LocalDate.of(2026, 8, 11)

    val schedulesByDate: Map<LocalDate, List<CalendarSchedule>> = mapOf(
        augustNinth to listOf(
            CalendarSchedule(
                id = 1,
                title = "저녁 약속",
                source = CalendarScheduleSource.DEVICE,
                startTime = LocalTime.of(23, 59),
                endTime = LocalTime.of(0, 0),
            ),
            CalendarSchedule(
                id = 2,
                title = "성심당 케이크 예약",
                source = CalendarScheduleSource.APP,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(13, 0),
                isAdded = true,
            ),
            CalendarSchedule(
                id = 3,
                title = "시간 없는 앱 일정",
                source = CalendarScheduleSource.APP,
                isAdded = true,
            ),
            CalendarSchedule(
                id = 4,
                title = "시간 없는 삼성 일정",
                source = CalendarScheduleSource.DEVICE,
            ),
            CalendarSchedule(
                id = 8,
                title = "KTX 예매",
                source = CalendarScheduleSource.APP,
                isAdded = false,
            ),
            CalendarSchedule(
                id = 9,
                title = "카페 예약 확인",
                source = CalendarScheduleSource.APP,
                isAdded = false,
            ),
        ),
        augustEleventh to listOf(
            CalendarSchedule(
                id = 5,
                title = "성심당 케이크 예약 성심당 케이크 예약 성심당 케이크 예약 성심당 케이크 예약",
                source = CalendarScheduleSource.APP,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(13, 0),
                isAdded = true,
            ),
            CalendarSchedule(
                id = 6,
                title = "KTX 예매",
                source = CalendarScheduleSource.APP,
                isAdded = false,
            ),
            CalendarSchedule(
                id = 7,
                title = "팀 미팅",
                source = CalendarScheduleSource.DEVICE,
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 30),
            ),
        ),
    )

    val scheduleCountByDate: Map<LocalDate, Int> = buildMap {
        put(LocalDate.of(2026, 7, 28), 1)
        put(LocalDate.of(2026, 7, 30), 1)
        put(LocalDate.of(2026, 7, 31), 1)
        put(LocalDate.of(2026, 8, 4), 1)
        put(LocalDate.of(2026, 8, 6), 1)
        put(LocalDate.of(2026, 8, 7), 1)
        putAll(schedulesByDate.mapValues { (_, schedules) -> schedules.size })
        put(LocalDate.of(2026, 8, 12), 3)
        put(LocalDate.of(2026, 8, 13), 1)
        put(LocalDate.of(2026, 8, 14), 1)
        put(LocalDate.of(2026, 8, 18), 1)
        put(LocalDate.of(2026, 8, 20), 1)
        put(LocalDate.of(2026, 8, 21), 1)
        put(LocalDate.of(2026, 8, 25), 1)
        put(LocalDate.of(2026, 8, 27), 1)
        put(LocalDate.of(2026, 8, 28), 1)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun CalendarScreenPreview() {
    val selectedDate = LocalDate.of(2026, 8, 9)

    ModeraTheme {
        CalendarScreen(
            visibleMonth = YearMonth.of(2026, 8),
            selectedDate = selectedDate,
            today = LocalDate.of(2026, 8, 10),
            scheduleCountByDate = CalendarDummyData.scheduleCountByDate,
            schedules = CalendarDummyData.schedulesByDate[selectedDate].orEmpty(),
            showYearPicker = false,
            onBackClick = {},
            onYearClick = {},
            onYearPickerDismiss = {},
            onYearSelect = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onDateClick = {},
            onEditClick = {},
            onScheduleClick = {},
            onAddScheduleClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun CalendarScreenYearPickerPreview() {
    ModeraTheme {
        CalendarScreen(
            visibleMonth = YearMonth.of(2026, 8),
            selectedDate = LocalDate.of(2026, 8, 9),
            today = LocalDate.of(2026, 8, 10),
            scheduleCountByDate = CalendarDummyData.scheduleCountByDate,
            schedules = emptyList(),
            showYearPicker = true,
            onBackClick = {},
            onYearClick = {},
            onYearPickerDismiss = {},
            onYearSelect = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onDateClick = {},
            onEditClick = {},
            onScheduleClick = {},
            onAddScheduleClick = {},
        )
    }
}
