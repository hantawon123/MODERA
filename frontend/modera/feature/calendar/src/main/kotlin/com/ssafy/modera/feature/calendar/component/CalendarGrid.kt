package com.ssafy.modera.feature.calendar.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.calendar.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    scheduleCountByDate: Map<LocalDate, Int>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val days = remember(visibleMonth) { visibleMonth.toCalendarDays() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        WeekdayHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
        )

        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                week.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        isCurrentMonth = YearMonth.from(date) == visibleMonth,
                        scheduleCount = scheduleCountByDate[date] ?: 0,
                        onClick = { onDateClick(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader(
    modifier: Modifier = Modifier,
) {
    val weekdays = listOf(
        stringResource(R.string.calendar_weekday_sunday) to ModeraTheme.colors.red,
        stringResource(R.string.calendar_weekday_monday) to ModeraTheme.colors.gray900,
        stringResource(R.string.calendar_weekday_tuesday) to ModeraTheme.colors.gray900,
        stringResource(R.string.calendar_weekday_wednesday) to ModeraTheme.colors.gray900,
        stringResource(R.string.calendar_weekday_thursday) to ModeraTheme.colors.gray900,
        stringResource(R.string.calendar_weekday_friday) to ModeraTheme.colors.gray900,
        stringResource(R.string.calendar_weekday_saturday) to ModeraTheme.colors.blue,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        weekdays.forEach { (label, color) ->
            WeekdayLabel(
                label = label,
                color = color,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WeekdayLabel(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = ModeraTheme.typography.captionM12,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

internal fun YearMonth.toCalendarDays(): List<LocalDate> {
    val firstOfMonth = atDay(1)
    val sundayOffset = firstOfMonth.dayOfWeek.value % DayOfWeek.SUNDAY.value
    val startDate = firstOfMonth.minusDays(sundayOffset.toLong())
    val daysNeeded = sundayOffset + lengthOfMonth()
    val weekCount = (daysNeeded + 6) / 7
    return List(weekCount * 7) { index -> startDate.plusDays(index.toLong()) }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CalendarGridPreview() {
    ModeraTheme {
        CalendarGrid(
            visibleMonth = YearMonth.of(2026, 8),
            selectedDate = LocalDate.of(2026, 8, 9),
            today = LocalDate.of(2026, 8, 10),
            scheduleCountByDate = mapOf(
                LocalDate.of(2026, 8, 11) to 5,
                LocalDate.of(2026, 8, 12) to 3,
                LocalDate.of(2026, 8, 4) to 1,
            ),
            onDateClick = {},
        )
    }
}
