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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.component.ModeraConfirmDialog
import com.ssafy.modera.core.component.ModeraTopBar
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
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
        isEditMode = uiState.isEditMode,
        scheduleToDelete = uiState.scheduleToDelete,
        onBackClick = onBackClick,
        onYearClick = viewModel::onYearClick,
        onTodayClick = viewModel::onTodayClick,
        onYearPickerDismiss = viewModel::onYearPickerDismiss,
        onYearSelect = viewModel::onYearSelect,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onDateClick = viewModel::onDateClick,
        onEditModeToggle = viewModel::onEditModeToggle,
        onScheduleClick = onScheduleClick,
        onAddScheduleClick = viewModel::onAddScheduleClick,
        onDeleteScheduleClick = viewModel::onDeleteScheduleClick,
        onDeleteDialogConfirm = viewModel::onDeleteDialogConfirm,
        onDeleteDialogDismiss = viewModel::onDeleteDialogDismiss,
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
    isEditMode: Boolean,
    scheduleToDelete: CalendarSchedule?,
    onBackClick: () -> Unit,
    onYearClick: () -> Unit,
    onTodayClick: () -> Unit,
    onYearPickerDismiss: () -> Unit,
    onYearSelect: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onEditModeToggle: () -> Unit,
    onScheduleClick: (CalendarSchedule) -> Unit,
    onAddScheduleClick: (CalendarSchedule) -> Unit,
    onDeleteScheduleClick: (CalendarSchedule) -> Unit,
    onDeleteDialogConfirm: () -> Unit,
    onDeleteDialogDismiss: () -> Unit,
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
            rightContent = {
                Text(
                    text = stringResource(R.string.calendar_today),
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.yellow800,
                    modifier = Modifier.clickable(onClick = onTodayClick),
                )
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
                isEditMode = isEditMode,
                onEditModeToggle = onEditModeToggle,
                onScheduleClick = onScheduleClick,
                onAddScheduleClick = onAddScheduleClick,
                onDeleteScheduleClick = onDeleteScheduleClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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

    scheduleToDelete?.let { schedule ->
        ModeraConfirmDialog(
            icon = painterResource(ModeraIcons.Trash),
            targetTitle = schedule.title,
            title = stringResource(R.string.calendar_schedule_delete_dialog_title),
            description = stringResource(R.string.calendar_schedule_delete_dialog_description),
            confirmText = stringResource(R.string.calendar_schedule_delete_dialog_confirm),
            dismissText = stringResource(R.string.calendar_schedule_delete_dialog_cancel),
            confirmButtonColor = ModeraTheme.colors.red,
            onConfirm = onDeleteDialogConfirm,
            onDismiss = onDeleteDialogDismiss,
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
            isEditMode = false,
            scheduleToDelete = null,
            onBackClick = {},
            onYearClick = {},
            onTodayClick = {},
            onYearPickerDismiss = {},
            onYearSelect = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onDateClick = {},
            onEditModeToggle = {},
            onScheduleClick = {},
            onAddScheduleClick = {},
            onDeleteScheduleClick = {},
            onDeleteDialogConfirm = {},
            onDeleteDialogDismiss = {},
        )
    }
}
