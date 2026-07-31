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
import com.ssafy.modera.feature.calendar.component.CalendarGrid
import com.ssafy.modera.feature.calendar.component.CalendarMonthHeader
import com.ssafy.modera.feature.calendar.component.CalendarScheduleSection
import com.ssafy.modera.feature.calendar.component.CalendarYearPickerDialog
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarRoute(
    onBackClick: () -> Unit,
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today) }
    var showYearPicker by remember { mutableStateOf(false) }
    val scheduleCountByDate = remember { CalendarDummyData.scheduleCountByDate }

    CalendarScreen(
        visibleMonth = visibleMonth,
        selectedDate = selectedDate,
        today = today,
        scheduleCountByDate = scheduleCountByDate,
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
        modifier = modifier,
    )
}

@Composable
fun CalendarScreen(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    scheduleCountByDate: Map<LocalDate, Int>,
    showYearPicker: Boolean,
    onBackClick: () -> Unit,
    onYearClick: () -> Unit,
    onYearPickerDismiss: () -> Unit,
    onYearSelect: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onEditClick: () -> Unit,
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

            Spacer(Modifier.height(10.dp))

            CalendarScheduleSection(
                selectedDate = selectedDate,
                onEditClick = onEditClick,
                modifier = Modifier.weight(1f)
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
    val scheduleCountByDate: Map<LocalDate, Int> = mapOf(
        LocalDate.of(2026, 7, 28) to 1,
        LocalDate.of(2026, 7, 30) to 1,
        LocalDate.of(2026, 7, 31) to 1,
        LocalDate.of(2026, 8, 4) to 1,
        LocalDate.of(2026, 8, 6) to 1,
        LocalDate.of(2026, 8, 7) to 1,
        LocalDate.of(2026, 8, 11) to 5,
        LocalDate.of(2026, 8, 12) to 3,
        LocalDate.of(2026, 8, 13) to 1,
        LocalDate.of(2026, 8, 14) to 1,
        LocalDate.of(2026, 8, 18) to 1,
        LocalDate.of(2026, 8, 20) to 1,
        LocalDate.of(2026, 8, 21) to 1,
        LocalDate.of(2026, 8, 25) to 1,
        LocalDate.of(2026, 8, 27) to 1,
        LocalDate.of(2026, 8, 28) to 1,
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun CalendarScreenPreview() {
    ModeraTheme {
        CalendarScreen(
            visibleMonth = YearMonth.of(2026, 8),
            selectedDate = LocalDate.of(2026, 8, 9),
            today = LocalDate.of(2026, 8, 10),
            scheduleCountByDate = CalendarDummyData.scheduleCountByDate,
            showYearPicker = false,
            onBackClick = {},
            onYearClick = {},
            onYearPickerDismiss = {},
            onYearSelect = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onDateClick = {},
            onEditClick = {},
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
            showYearPicker = true,
            onBackClick = {},
            onYearClick = {},
            onYearPickerDismiss = {},
            onYearSelect = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onDateClick = {},
            onEditClick = {},
        )
    }
}
