package com.ssafy.modera.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.data.repository.calendar.DeviceCalendarRepository
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.core.model.calendar.CalendarScheduleSource
import com.ssafy.modera.feature.calendar.state.CalendarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val deviceCalendarRepository: DeviceCalendarRepository,
) : ViewModel() {

    private val today = LocalDate.now()
    private val visibleMonth = MutableStateFlow(YearMonth.from(today))
    private val selectedDate = MutableStateFlow(today)
    private val showYearPicker = MutableStateFlow(false)
    private val isEditMode = MutableStateFlow(false)
    private val scheduleToDelete = MutableStateFlow<CalendarSchedule?>(null)
    private val hasCalendarPermission = MutableStateFlow(false)
    private val appSchedulesByDate = MutableStateFlow(CalendarDummyData.appSchedulesByDate)

    private val navigationState = combine(
        visibleMonth,
        selectedDate,
        showYearPicker,
        isEditMode,
        scheduleToDelete,
    ) { month, date, yearPickerVisible, editMode, pendingDelete ->
        NavigationState(
            visibleMonth = month,
            selectedDate = date,
            showYearPicker = yearPickerVisible,
            isEditMode = editMode,
            scheduleToDelete = pendingDelete,
        )
    }

    private val deviceScheduleCounts = combine(
        hasCalendarPermission,
        visibleMonth,
    ) { granted, month ->
        granted to month
    }.flatMapLatest { (granted, month) ->
        if (granted) {
            deviceCalendarRepository.getScheduleCountsForMonth(month)
        } else {
            flowOf(emptyMap())
        }
    }

    private val deviceSchedulesForSelectedDate = combine(
        hasCalendarPermission,
        selectedDate,
    ) { granted, date ->
        granted to date
    }.flatMapLatest { (granted, date) ->
        if (granted) {
            deviceCalendarRepository.getSchedulesForDate(date)
        } else {
            flowOf(emptyList())
        }
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        navigationState,
        deviceScheduleCounts,
        deviceSchedulesForSelectedDate,
        appSchedulesByDate,
    ) { navigation, deviceCounts, deviceSchedules, appByDate ->
        val appSchedules = appByDate[navigation.selectedDate].orEmpty()
        val appCounts = appByDate.mapValues { (_, schedules) -> schedules.size }

        CalendarUiState(
            visibleMonth = navigation.visibleMonth,
            selectedDate = navigation.selectedDate,
            today = today,
            appScheduleCountByDate = appCounts,
            deviceScheduleCountByDate = deviceCounts,
            schedules = deviceSchedules + appSchedules,
            showYearPicker = navigation.showYearPicker,
            isEditMode = navigation.isEditMode,
            scheduleToDelete = navigation.scheduleToDelete,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(
            visibleMonth = YearMonth.from(today),
            selectedDate = today,
            today = today,
            appScheduleCountByDate = emptyMap(),
            deviceScheduleCountByDate = emptyMap(),
            schedules = emptyList(),
            showYearPicker = false,
        ),
    )

    fun onCalendarPermissionResult(granted: Boolean) {
        hasCalendarPermission.value = granted
    }

    fun onPreviousMonth() {
        visibleMonth.update { it.minusMonths(1) }
    }

    fun onNextMonth() {
        visibleMonth.update { it.plusMonths(1) }
    }

    fun onDateClick(date: LocalDate) {
        selectedDate.value = date
        visibleMonth.value = YearMonth.from(date)
    }

    fun onYearClick() {
        showYearPicker.value = true
    }

    fun onYearPickerDismiss() {
        showYearPicker.value = false
    }

    fun onYearSelect(year: Int) {
        visibleMonth.update { it.withYear(year) }
        selectedDate.update { it.withYear(year) }
        showYearPicker.value = false
    }

    fun onEditModeToggle() {
        isEditMode.update { editing ->
            if (editing) {
                scheduleToDelete.value = null
            }
            !editing
        }
    }

    fun onDeleteScheduleClick(schedule: CalendarSchedule) {
        scheduleToDelete.value = schedule
    }

    fun onDeleteDialogDismiss() {
        scheduleToDelete.value = null
    }

    fun onDeleteDialogConfirm() {
        val target = scheduleToDelete.value ?: return
        val date = selectedDate.value
        appSchedulesByDate.update { current ->
            current.mapValues { (scheduleDate, schedules) ->
                if (scheduleDate != date) {
                    schedules
                } else {
                    schedules.filterNot { it.id == target.id }
                }
            }
        }
        scheduleToDelete.value = null
    }

    fun onAddScheduleClick(schedule: CalendarSchedule) {
        val date = selectedDate.value
        appSchedulesByDate.update { current ->
            current.mapValues { (scheduleDate, schedules) ->
                if (scheduleDate != date) {
                    schedules
                } else {
                    schedules.map { item ->
                        if (item.id == schedule.id) {
                            item.copy(isAdded = true)
                        } else {
                            item
                        }
                    }
                }
            }
        }
    }

    private data class NavigationState(
        val visibleMonth: YearMonth,
        val selectedDate: LocalDate,
        val showYearPicker: Boolean,
        val isEditMode: Boolean,
        val scheduleToDelete: CalendarSchedule?,
    )
}

internal object CalendarDummyData {
    private val augustNinth = LocalDate.of(2026, 8, 9)
    private val augustEleventh = LocalDate.of(2026, 8, 11)

    val appSchedulesByDate: Map<LocalDate, List<CalendarSchedule>> = mapOf(
        augustNinth to listOf(
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
                id = 8,
                title = "KTX 예매",
                source = CalendarScheduleSource.APP,
                isAdded = false,
            ),
            CalendarSchedule(
                id = 9,
                title = "카페 예약 확인",
                source = CalendarScheduleSource.APP,
                isAdded = false,
            ),
        ),
        augustEleventh to listOf(
            CalendarSchedule(
                id = 5,
                title = "성심당 케이크 예약 성심당 케이크 예약 성심당 케이크 예약 성심당 케이크 예약",
                source = CalendarScheduleSource.APP,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(13, 0),
                isAdded = true,
            ),
            CalendarSchedule(
                id = 6,
                title = "KTX 예매",
                source = CalendarScheduleSource.APP,
                isAdded = false,
            ),
        ),
    )
}
