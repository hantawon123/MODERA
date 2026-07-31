package com.ssafy.modera.feature.calendar.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.calendar.R
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun CalendarScheduleSection(
    selectedDate: LocalDate,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ModeraTheme.colors.gray50),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
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
            )

            Text(
                text = stringResource(R.string.calendar_edit),
                style = ModeraTheme.typography.bodySB14,
                color = ModeraTheme.colors.blue,
                modifier = Modifier.clickable(onClick = onEditClick),
            )
        }

        Text(
            text = stringResource(R.string.calendar_empty_schedule),
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray500,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))
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
private fun CalendarScheduleSectionPreview() {
    ModeraTheme {
        CalendarScheduleSection(
            selectedDate = LocalDate.of(2026, 8, 9),
            onEditClick = {},
        )
    }
}
