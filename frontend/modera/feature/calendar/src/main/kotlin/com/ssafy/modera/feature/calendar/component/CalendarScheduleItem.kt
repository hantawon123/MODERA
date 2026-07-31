package com.ssafy.modera.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.model.calendar.CalendarScheduleSource
import com.ssafy.modera.feature.calendar.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun CalendarScheduleItem(
    schedule: CalendarSchedule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColor = schedule.source.barColor()
    val formattedStart = remember(schedule.startTime) {
        schedule.startTime?.format(TimeFormatter)
    }
    val formattedEnd = remember(schedule.endTime) {
        schedule.endTime?.format(TimeFormatter)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ScheduleTimeColumn(
            startTime = formattedStart,
            endTime = formattedEnd,
            modifier = Modifier.width(36.dp),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(barColor, RoundedCornerShape(2.dp)),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = schedule.title,
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray900,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
        )

        if (schedule.source == CalendarScheduleSource.APP) {
            Spacer(modifier = Modifier.width(8.dp))
            if (schedule.isAdded) {
                Icon(
                    imageVector = ImageVector.vectorResource(ModeraIcons.ArrowRightCircle),
                    contentDescription = stringResource(
                        R.string.calendar_schedule_detail_content_description,
                    ),
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterVertically),
                    tint = ModeraTheme.colors.gray500,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterVertically)
                        .clip(CircleShape)
                        .background(ModeraTheme.colors.yellow700),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(ModeraIcons.Add),
                        contentDescription = stringResource(
                            R.string.calendar_schedule_add_content_description,
                        ),
                        modifier = Modifier.size(14.dp),
                        tint = ModeraTheme.colors.white,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleTimeColumn(
    startTime: String?,
    endTime: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.End,
    ) {
        if (startTime != null) {
            Text(
                text = startTime,
                style = ModeraTheme.typography.captionR12,
                color = ModeraTheme.colors.gray700,
            )
            if (endTime != null) {
                Text(
                    text = endTime,
                    style = ModeraTheme.typography.captionR12,
                    color = ModeraTheme.colors.gray700,
                )
            }
        }
    }
}

@Composable
private fun CalendarScheduleSource.barColor(): Color = when (this) {
    CalendarScheduleSource.APP -> ModeraTheme.colors.yellow700
    CalendarScheduleSource.DEVICE -> ModeraTheme.colors.brown
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CalendarScheduleItemAppTimedPreview() {
    ModeraTheme {
        CalendarScheduleItem(
            schedule = CalendarSchedule(
                id = 1,
                title = "성심당 케이크 예약",
                source = CalendarScheduleSource.APP,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(13, 0),
                isAdded = true,
            ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CalendarScheduleItemDeviceTimedPreview() {
    ModeraTheme {
        CalendarScheduleItem(
            schedule = CalendarSchedule(
                id = 2,
                title = "저녁 약속",
                source = CalendarScheduleSource.DEVICE,
                startTime = LocalTime.of(23, 59),
                endTime = LocalTime.of(0, 0),
            ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CalendarScheduleItemLongTitlePreview() {
    ModeraTheme {
        CalendarScheduleItem(
            schedule = CalendarSchedule(
                id = 3,
                title = "성심당 케이크 예약 성심당 케이크 예약 성심당 케이크 예약 성심당 케이크 예약",
                source = CalendarScheduleSource.APP,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(13, 0),
                isAdded = true,
            ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CalendarScheduleItemUntimedAddPreview() {
    ModeraTheme {
        CalendarScheduleItem(
            schedule = CalendarSchedule(
                id = 4,
                title = "KTX 예매 일정",
                source = CalendarScheduleSource.APP,
                isAdded = false,
            ),
            onClick = {},
        )
    }
}
