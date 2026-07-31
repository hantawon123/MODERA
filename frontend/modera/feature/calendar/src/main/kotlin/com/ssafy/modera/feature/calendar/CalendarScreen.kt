package com.ssafy.modera.feature.calendar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.model.calendar.CalendarScheduleSource
import com.ssafy.modera.feature.calendar.component.CalendarGrid
import com.ssafy.modera.feature.calendar.component.CalendarMonthHeader
import com.ssafy.modera.feature.calendar.component.CalendarScheduleSection
import com.ssafy.modera.feature.calendar.component.CalendarYearPickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@Composable
fun CalendarRoute(
    onBackClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onScheduleClick: (CalendarSchedule) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onCalendarPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            viewModel.onCalendarPermissionResult(true)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    CalendarScreen(
        visibleMonth = uiState.visibleMonth,
        selectedDate = uiState.selectedDate,
        today = uiState.today,
        appScheduleCountByDate = uiState.appScheduleCountByDate,
        deviceScheduleCountByDate = uiState.deviceScheduleCountByDate,
        schedules = uiState.schedules,
        showYearPicker = uiState.showYearPicker,
        onBackClick = onBackClick,
        onYearClick = viewModel::onYearClick,
        onYearPickerDismiss = viewModel::onYearPickerDismiss,
        onYearSelect = viewModel::onYearSelect,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onDateClick = viewModel::onDateClick,
        onEditClick = onEditClick,
        onScheduleClick = onScheduleClick,
        onAddScheduleClick = viewModel::onAddScheduleClick,
        modifier = modifier,
    )
}

@Composable
fun CalendarScreen(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    appScheduleCountByDate: Map<LocalDate, Int>,
    deviceScheduleCountByDate: Map<LocalDate, Int>,
    schedules: List<CalendarSchedule>,
    showYearPicker: Boolean,
    onBackClick: () -> Unit,
    onYearClick: () -> Unit,
    onYearPickerDismiss: () -> Unit,
    onYearSelect: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onEditClick: () -> Unit,
    onScheduleClick: (CalendarSchedule) -> Unit,
    onAddScheduleClick: (CalendarSchedule) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = ModeraTheme.colors.white),
    ) {
        ModeraTopBar(
            onBackClick = onBackClick,
            centerContent = {
                Text(
                    text = visibleMonth.year.toString(),
                    style = ModeraTheme.typography.bodySB16,
                    color = ModeraTheme.colors.gray900,
                    modifier = Modifier.clickable(onClick = onYearClick),
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            CalendarMonthHeader(
                visibleMonth = visibleMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
            )

            CalendarGrid(
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                today = today,
                appScheduleCountByDate = appScheduleCountByDate,
                deviceScheduleCountByDate = deviceScheduleCountByDate,
                onDateClick = onDateClick,
            )

            Spacer(modifier = Modifier.height(10.dp))

            CalendarScheduleSection(
                selectedDate = selectedDate,
                schedules = schedules,
                onEditClick = onEditClick,
                onScheduleClick = onScheduleClick,
                onAddScheduleClick = onAddScheduleClick,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showYearPicker) {
        CalendarYearPickerDialog(
            selectedYear = visibleMonth.year,
            onYearSelect = onYearSelect,
            onDismissRequest = onYearPickerDismiss,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun CalendarScreenPreview() {
    ModeraTheme {
        CalendarScreen(
            visibleMonth = YearMonth.of(2026, 8),
            selectedDate = LocalDate.of(2026, 8, 9),
            today = LocalDate.of(2026, 8, 10),
            appScheduleCountByDate = mapOf(LocalDate.of(2026, 8, 9) to 2),
            deviceScheduleCountByDate = mapOf(LocalDate.of(2026, 8, 9) to 1),
            schedules = listOf(
                CalendarSchedule(
                    id = 1,
                    title = "저녁 약속",
                    source = CalendarScheduleSource.DEVICE,
                    startTime = LocalTime.of(19, 0),
                    endTime = LocalTime.of(21, 0),
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
                    id = 8,
                    title = "KTX 예매",
                    source = CalendarScheduleSource.APP,
                    isAdded = false,
                ),
            ),
            showYearPicker = false,
            onBackClick = {},
            onYearClick = {},
            onYearPickerDismiss = {},
            onYearSelect = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onDateClick = {},
            onEditClick = {},
            onScheduleClick = {},
            onAddScheduleClick = {},
        )
    }
}
