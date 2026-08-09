package com.ssafy.modera.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.calendar.R
import java.time.DayOfWeek
import java.time.LocalDate

private const val MaxVisibleDots = 4

@Composable
fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    appScheduleCount: Int,
    deviceScheduleCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayOfWeek = date.dayOfWeek
    val dayTextColor = dayTextColor(
        dayOfWeek = dayOfWeek,
        isToday = isToday,
        isCurrentMonth = isCurrentMonth,
    )
    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 1.dp,
            color = ModeraTheme.colors.gray300,
            shape = RoundedCornerShape(8.dp),
        )
    } else {
        Modifier
    }
    val dateBackgroundColor = if (isToday) ModeraTheme.colors.gray500 else ModeraTheme.colors.white

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(onClick = onClick)
            .then(borderModifier)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .background(dateBackgroundColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 4.dp),
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = ModeraTheme.typography.bodyR14,
                color = dayTextColor,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))

        ScheduleDots(
            appScheduleCount = appScheduleCount,
            deviceScheduleCount = deviceScheduleCount,
        )
    }
}

@Composable
private fun ScheduleDots(
    appScheduleCount: Int,
    deviceScheduleCount: Int,
    modifier: Modifier = Modifier,
) {
    val appDotColor = ModeraTheme.colors.yellow700
    val deviceDotColor = ModeraTheme.colors.brown
    val dotColors = remember(appScheduleCount, deviceScheduleCount, appDotColor, deviceDotColor) {
        buildList {
            repeat(deviceScheduleCount) { add(deviceDotColor) }
            repeat(appScheduleCount) { add(appDotColor) }
        }
    }

    if (dotColors.isEmpty()) {
        Spacer(modifier = modifier.fillMaxWidth())
        return
    }

    val visibleDots = dotColors.take(MaxVisibleDots)
    val overflow = dotColors.size - MaxVisibleDots

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visibleDots.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }

        if (overflow > 0) {
            Text(
                text = stringResource(R.string.calendar_more_schedules, overflow),
                style = ModeraTheme.typography.captionR10,
                color = ModeraTheme.colors.gray500,
            )
        }
    }
}

@Composable
private fun dayTextColor(
    dayOfWeek: DayOfWeek,
    isToday: Boolean,
    isCurrentMonth: Boolean,
): Color {
    if (isToday) return ModeraTheme.colors.white

    val baseColor = when (dayOfWeek) {
        DayOfWeek.SUNDAY -> ModeraTheme.colors.red
        DayOfWeek.SATURDAY -> ModeraTheme.colors.blue
        else -> ModeraTheme.colors.gray900
    }

    return if (isCurrentMonth) baseColor else ModeraTheme.colors.gray400
}

@Preview(showBackground = true)
@Composable
private fun CalendarDayCellSelectedPreview() {
    ModeraTheme {
        CalendarDayCell(
            date = LocalDate.of(2026, 8, 9),
            isSelected = true,
            isToday = false,
            isCurrentMonth = true,
            appScheduleCount = 0,
            deviceScheduleCount = 0,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarDayCellTodayPreview() {
    ModeraTheme {
        CalendarDayCell(
            date = LocalDate.of(2026, 8, 10),
            isSelected = false,
            isToday = true,
            isCurrentMonth = true,
            appScheduleCount = 0,
            deviceScheduleCount = 0,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarDayCellOverflowPreview() {
    ModeraTheme {
        CalendarDayCell(
            date = LocalDate.of(2026, 8, 11),
            isSelected = false,
            isToday = false,
            isCurrentMonth = true,
            appScheduleCount = 3,
            deviceScheduleCount = 2,
            onClick = {},
        )
    }
}
