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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.calendar.R

private const val YearsPerPage = 8
private const val YearColumns = 4
private const val YearPageOrigin = 2021

internal fun yearPageStart(year: Int): Int {
    val pageIndex = Math.floorDiv(year - YearPageOrigin, YearsPerPage)
    return YearPageOrigin + pageIndex * YearsPerPage
}

@Composable
fun CalendarYearPickerDialog(
    selectedYear: Int,
    onYearSelect: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pageStartYear by remember(selectedYear) {
        mutableIntStateOf(yearPageStart(selectedYear))
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        CalendarYearPickerContent(
            selectedYear = selectedYear,
            pageStartYear = pageStartYear,
            onPreviousPage = { pageStartYear -= YearsPerPage },
            onNextPage = { pageStartYear += YearsPerPage },
            onYearSelect = onYearSelect,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
        )
    }
}

@Composable
internal fun CalendarYearPickerContent(
    selectedYear: Int,
    pageStartYear: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onYearSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val years = remember(pageStartYear) {
        List(YearsPerPage) { index -> pageStartYear + index }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ModeraTheme.colors.white)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(ModeraIcons.ArrowLeft),
                contentDescription = stringResource(
                    R.string.calendar_previous_year_page_content_description,
                ),
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onPreviousPage),
                tint = ModeraTheme.colors.gray700,
            )

            Icon(
                imageVector = ImageVector.vectorResource(ModeraIcons.ArrowRight),
                contentDescription = stringResource(
                    R.string.calendar_next_year_page_content_description,
                ),
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onNextPage),
                tint = ModeraTheme.colors.gray700,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            years.chunked(YearColumns).forEach { rowYears ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowYears.forEach { year ->
                        YearChip(
                            year = year,
                            selected = year == selectedYear,
                            onClick = { onYearSelect(year) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YearChip(
    year: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(ModeraTheme.colors.gray100)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.dp,
                        color = ModeraTheme.colors.gray700,
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = year.toString(),
            style = ModeraTheme.typography.bodyR14,
            color = ModeraTheme.colors.gray900,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun CalendarYearPickerContentPreview() {
    ModeraTheme {
        CalendarYearPickerContent(
            selectedYear = 2026,
            pageStartYear = 2021,
            onPreviousPage = {},
            onNextPage = {},
            onYearSelect = {},
        )
    }
}
