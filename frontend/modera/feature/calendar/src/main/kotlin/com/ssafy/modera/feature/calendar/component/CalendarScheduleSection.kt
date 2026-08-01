package com.ssafy.modera.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.ssafy.modera.core.model.calendar.editableSchedules
import com.ssafy.modera.core.model.calendar.pendingSchedules
import com.ssafy.modera.feature.calendar.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun CalendarScheduleSection(
    selectedDate: LocalDate,
    schedules: List<CalendarSchedule>,
    isEditMode: Boolean,
    onEditModeToggle: () -> Unit,
    onScheduleClick: (CalendarSchedule) -> Unit,
    onAddScheduleClick: (CalendarSchedule) -> Unit,
    onDeleteScheduleClick: (CalendarSchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val addedSchedules = remember(schedules) { schedules.addedSchedules() }
    val pendingSchedules = remember(schedules) { schedules.pendingSchedules() }
    val editableSchedules = remember(schedules) { schedules.editableSchedules() }
    val visibleSchedules = if (isEditMode) editableSchedules else addedSchedules

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(ModeraTheme.colors.gray100),
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
                text = stringResource(
                    if (isEditMode) {
                        R.string.calendar_edit_done
                    } else {
                        R.string.calendar_edit
                    },
                ),
                style = ModeraTheme.typography.bodySB14,
                color = ModeraTheme.colors.blue,
                modifier = Modifier.clickable(onClick = onEditModeToggle),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            if (visibleSchedules.isEmpty()) {
                Text(
                    text = stringResource(R.string.calendar_empty_schedule),
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.gray500,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                Spacer(modifier = Modifier.height(14.dp))
            } else {
                visibleSchedules.forEach { schedule ->
                    CalendarScheduleItem(
                        schedule = schedule,
                        isEditMode = isEditMode,
                        onClick = {
                            if (isEditMode) {
                                onDeleteScheduleClick(schedule)
                            } else {
                                onScheduleClick(schedule)
                            }
                        },
                        onDeleteClick = if (isEditMode) {
                            { onDeleteScheduleClick(schedule) }
                        } else {
                            null
                        },
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (!isEditMode && pendingSchedules.isNotEmpty()) {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = ModeraTheme.colors.gray300,
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
private fun CalendarScheduleSectionEditModePreview() {
    ModeraTheme {
        CalendarScheduleSection(
            selectedDate = LocalDate.of(2026, 8, 9),
            schedules = listOf(
                CalendarSchedule(
                    id = 1,
                    title = "공주님 생일!",
                    source = CalendarScheduleSource.DEVICE,
                ),
                CalendarSchedule(
                    id = 2,
                    title = "SSAFY 중간 발표",
                    source = CalendarScheduleSource.APP,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(13, 0),
                    isAdded = true,
                ),
                CalendarSchedule(
                    id = 3,
                    title = "SSAFY 기획서 제출",
                    source = CalendarScheduleSource.APP,
                    isAdded = true,
                ),
                CalendarSchedule(
                    id = 4,
                    title = "KTX 예매",
                    source = CalendarScheduleSource.APP,
                    isAdded = false,
                ),
            ),
            isEditMode = true,
            onEditModeToggle = {},
            onScheduleClick = {},
            onAddScheduleClick = {},
            onDeleteScheduleClick = {},
        )
    }
}
