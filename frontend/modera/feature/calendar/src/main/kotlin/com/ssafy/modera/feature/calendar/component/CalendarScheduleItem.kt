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
    isEditMode: Boolean = false,
    onDeleteClick: (() -> Unit)? = null,
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
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ScheduleTimeColumn(
            startTime = formattedStart,
            endTime = formattedEnd,
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp),
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

        when {
            isEditMode && onDeleteClick != null -> {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = ImageVector.vectorResource(ModeraIcons.Trash),
                    contentDescription = stringResource(
                        R.string.calendar_schedule_delete_content_description,
                    ),
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterVertically)
                        .clickable(onClick = onDeleteClick),
                    tint = ModeraTheme.colors.red,
                )
            }

            schedule.source == CalendarScheduleSource.APP -> {
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
}

@Composable
private fun ScheduleTimeColumn(
    startTime: String?,
    endTime: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        if (startTime != null) {
            listOfNotNull(
                startTime.takeUnless { it == "00:00" },
                endTime,
            ).forEachIndexed { index, time ->
                if (index > 0) {
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = time,
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.gray500,
                )
            }
        } else {
            Text(
                text = "종일",
                style = ModeraTheme.typography.bodyR14,
                color = ModeraTheme.colors.gray500,
            )
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
private fun CalendarScheduleItemEditModePreview() {
    ModeraTheme {
        CalendarScheduleItem(
            schedule = CalendarSchedule(
                id = 1,
                title = "SSAFY 중간 발표",
                source = CalendarScheduleSource.APP,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(13, 0),
                isAdded = true,
            ),
            onClick = {},
            isEditMode = true,
            onDeleteClick = {},
        )
    }
}
