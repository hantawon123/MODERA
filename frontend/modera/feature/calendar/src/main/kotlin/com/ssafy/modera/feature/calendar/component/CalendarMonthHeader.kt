package com.ssafy.modera.feature.calendar.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.calendar.R
import com.ssafy.modera.feature.calendar.R.drawable.ic_arrow_left_circle_20
import com.ssafy.modera.feature.calendar.R.drawable.ic_arrow_right_circle_20
import java.time.YearMonth

@Composable
fun CalendarMonthHeader(
    visibleMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(ic_arrow_left_circle_20),
            contentDescription = stringResource(R.string.calendar_previous_month_content_description),
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onPreviousMonth),
            tint = Color.Unspecified,
        )

        Text(
            text = stringResource(R.string.calendar_month_label, visibleMonth.monthValue),
            style = ModeraTheme.typography.titleSB20,
            color = ModeraTheme.colors.gray900,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Icon(
            imageVector = ImageVector.vectorResource(ic_arrow_right_circle_20),
            contentDescription = stringResource(R.string.calendar_next_month_content_description),
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onNextMonth),
            tint = Color.Unspecified,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarMonthHeaderPreview() {
    ModeraTheme {
        CalendarMonthHeader(
            visibleMonth = YearMonth.of(2026, 8),
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}
