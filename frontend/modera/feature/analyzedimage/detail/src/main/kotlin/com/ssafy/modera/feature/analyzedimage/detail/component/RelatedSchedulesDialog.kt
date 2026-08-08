package com.ssafy.modera.feature.analyzedimage.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ssafy.modera.core.common.datetime.ModeraDateFormatter
import com.ssafy.modera.core.common.datetime.ModeraDateStyle
import com.ssafy.modera.core.designsystem.component.HorizontalDivider
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.model.calendar.CalendarScheduleSource
import com.ssafy.modera.feature.analyzedimage.detail.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
internal fun RelatedSchedulesDialog(
    schedules: List<CalendarSchedule>,
    onScheduleClick: (CalendarSchedule) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ModeraTheme.colors.white)
                .padding(24.dp),
        ) {
            Icon(
                painter = painterResource(ModeraIcons.CalendarNumber),
                contentDescription = null,
                tint = ModeraTheme.colors.yellow800,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.analyzed_image_detail_related_schedules_dialog_title),
                style = ModeraTheme.typography.bodySB16,
                color = ModeraTheme.colors.gray900,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(
                    items = schedules,
                    key = CalendarSchedule::id,
                ) { schedule ->
                    RelatedScheduleDialogItem(
                        schedule = schedule,
                        onClick = { onScheduleClick(schedule) },
                    )

                    if (schedule != schedules.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedScheduleDialogItem(
    schedule: CalendarSchedule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val subtitle = remember(schedule.date, schedule.startTime, schedule.endTime) {
        buildScheduleSubtitle(
            date = schedule.date,
            startTime = schedule.startTime,
            endTime = schedule.endTime,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = schedule.title,
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray900,
        )

        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = ModeraTheme.typography.captionR12,
                color = ModeraTheme.colors.gray500,
            )
        }
    }
}

private fun buildScheduleSubtitle(
    date: LocalDate?,
    startTime: LocalTime?,
    endTime: LocalTime?,
): String {
    val dateText = date?.let { scheduleDate ->
        ModeraDateFormatter.formatMillis(
            timestampMillis = scheduleDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            style = ModeraDateStyle.YEAR_MONTH_DAY,
        )
    }.orEmpty()

    val timeText = when {
        startTime != null && endTime != null -> {
            "${startTime.format(TimeFormatter)} - ${endTime.format(TimeFormatter)}"
        }

        startTime != null -> startTime.format(TimeFormatter)
        endTime != null -> endTime.format(TimeFormatter)
        else -> null
    }

    return listOfNotNull(dateText, timeText)
        .filter(String::isNotBlank)
        .joinToString(" · ")
}

@Preview(showBackground = true)
@Composable
private fun RelatedSchedulesDialogPreview() {
    ModeraTheme {
        RelatedSchedulesDialog(
            schedules = listOf(
                CalendarSchedule(
                    id = 1,
                    title = "성심당 케이크 예약",
                    source = CalendarScheduleSource.APP,
                    date = LocalDate.of(2026, 8, 9),
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(13, 0),
                    isAdded = true,
                ),
                CalendarSchedule(
                    id = 2,
                    title = "저녁 약속",
                    source = CalendarScheduleSource.APP,
                    date = LocalDate.of(2026, 8, 15),
                    startTime = LocalTime.of(19, 0),
                    endTime = LocalTime.of(21, 0),
                    isAdded = true,
                ),
            ),
            onScheduleClick = {},
            onDismiss = {},
        )
    }
}
