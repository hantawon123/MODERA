package com.ssafy.modera.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.model.calendar.CalendarScheduleSource
import com.ssafy.modera.core.model.calendar.addedSchedules
import com.ssafy.modera.core.model.calendar.pendingSchedules
import com.ssafy.modera.feature.calendar.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun CalendarScheduleSection(
    selectedDate: LocalDate,
    schedules: List<CalendarSchedule>,
    onEditClick: () -> Unit,
    onScheduleClick: (CalendarSchedule) -> Unit,
    onAddScheduleClick: (CalendarSchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val addedSchedules = remember(schedules) { schedules.addedSchedules() }
    val pendingSchedules = remember(schedules) { schedules.pendingSchedules() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ModeraTheme.colors.gray50),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.calendar_selected_date_label,
                    selectedDate.dayOfMonth,
                    selectedDate.dayOfWeek.toDisplayName(),
                ),
                style = ModeraTheme.typography.titleSB20,
                color = ModeraTheme.colors.gray900,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = stringResource(R.string.calendar_edit),
                style = ModeraTheme.typography.bodySB14,
                color = ModeraTheme.colors.blue,
                modifier = Modifier.clickable(onClick = onEditClick),
            )
        }

        if (addedSchedules.isEmpty()) {
            Text(
                text = stringResource(R.string.calendar_empty_schedule),
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray500,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))
        } else {
            addedSchedules.forEach { schedule ->
                CalendarScheduleItem(
                    schedule = schedule,
                    onClick = { onScheduleClick(schedule) },
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (pendingSchedules.isNotEmpty()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = ModeraTheme.colors.gray100,
            )

            Text(
                text = stringResource(R.string.calendar_recognized_schedules),
                style = ModeraTheme.typography.bodySB14,
                color = ModeraTheme.colors.gray700,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            pendingSchedules.forEach { schedule ->
                CalendarScheduleItem(
                    schedule = schedule,
                    onClick = { onAddScheduleClick(schedule) },
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun DayOfWeek.toDisplayName(): String = stringResource(
    when (this) {
        DayOfWeek.SUNDAY -> R.string.calendar_day_sunday
        DayOfWeek.MONDAY -> R.string.calendar_day_monday
        DayOfWeek.TUESDAY -> R.string.calendar_day_tuesday
        DayOfWeek.WEDNESDAY -> R.string.calendar_day_wednesday
        DayOfWeek.THURSDAY -> R.string.calendar_day_thursday
        DayOfWeek.FRIDAY -> R.string.calendar_day_friday
        DayOfWeek.SATURDAY -> R.string.calendar_day_saturday
    },
)

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CalendarScheduleSectionEmptyPreview() {
    ModeraTheme {
        CalendarScheduleSection(
            selectedDate = LocalDate.of(2026, 8, 9),
            schedules = emptyList(),
            onEditClick = {},
            onScheduleClick = {},
            onAddScheduleClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CalendarScheduleSectionFilledPreview() {
    ModeraTheme {
        CalendarScheduleSection(
            selectedDate = LocalDate.of(2026, 8, 9),
            schedules = listOf(
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
                    id = 5,
                    title = "KTX 예매",
                    source = CalendarScheduleSource.APP,
                    isAdded = false,
                ),
                CalendarSchedule(
                    id = 6,
                    title = "카페 예약 확인",
                    source = CalendarScheduleSource.APP,
                    isAdded = false,
                ),
            ),
            onEditClick = {},
            onScheduleClick = {},
            onAddScheduleClick = {},
        )
    }
}
