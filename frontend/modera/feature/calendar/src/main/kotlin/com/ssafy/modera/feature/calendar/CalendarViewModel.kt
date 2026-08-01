package com.ssafy.modera.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.domain.calendar.GetCalendarSchedulesUseCase
import com.ssafy.modera.core.model.calendar.CalendarSchedule
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
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCalendarSchedulesUseCase: GetCalendarSchedulesUseCase,
) : ViewModel() {

    private val today = LocalDate.now()
    private val visibleMonth = MutableStateFlow(YearMonth.from(today))
    private val selectedDate = MutableStateFlow(today)
    private val showYearPicker = MutableStateFlow(false)
    private val isEditMode = MutableStateFlow(false)
    private val scheduleToDelete = MutableStateFlow<CalendarSchedule?>(null)
    private val hasCalendarPermission = MutableStateFlow(false)
    private val localScheduleChanges = MutableStateFlow<Map<Long, LocalScheduleChange>>(emptyMap())

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

    private val appSchedules = visibleMonth
        .flatMapLatest { month ->
            getCalendarSchedulesUseCase.appSchedulesForVisibleGrid(
                visibleMonth = month,
                selectedDate = selectedDate.value,
            )
        }

    private val deviceScheduleCounts = combine(
        hasCalendarPermission,
        visibleMonth,
    ) { granted, month ->
        granted to month
    }.flatMapLatest { (granted, month) ->
        if (granted) {
            getCalendarSchedulesUseCase.deviceScheduleCountsForVisibleGrid(month)
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
            getCalendarSchedulesUseCase.deviceSchedulesForDate(date)
        } else {
            flowOf(emptyList())
        }
    }

    private val deviceScheduleState = combine(
        deviceScheduleCounts,
        deviceSchedulesForSelectedDate,
    ) { counts, schedules ->
        DeviceScheduleState(
            counts = counts,
            schedules = schedules,
        )
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        navigationState,
        appSchedules,
        deviceScheduleState,
        localScheduleChanges,
    ) { navigation, appSchedules, deviceSchedule, localChanges ->
        val appSchedulesForSelectedDate = appSchedules
            .filter { it.date == navigation.selectedDate }
            .applyLocalChanges(localChanges)

        CalendarUiState(
            visibleMonth = navigation.visibleMonth,
            selectedDate = navigation.selectedDate,
            today = today,
            appScheduleCountByDate = appSchedules.toCountByDate(),
            deviceScheduleCountByDate = deviceSchedule.counts,
            schedules = deviceSchedule.schedules + appSchedulesForSelectedDate,
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
        if (YearMonth.from(date) != visibleMonth.value) {
            visibleMonth.value = YearMonth.from(date)
        }
    }

    fun onTodayClick() {
        onDateClick(today)
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
        localScheduleChanges.update { current ->
            current + (target.id to LocalScheduleChange.Removed)
        }
        scheduleToDelete.value = null
    }

    fun onAddScheduleClick(schedule: CalendarSchedule) {
        localScheduleChanges.update { current ->
            current + (schedule.id to LocalScheduleChange.Updated(schedule.copy(isAdded = true)))
        }
    }

    private fun List<CalendarSchedule>.applyLocalChanges(
        changes: Map<Long, LocalScheduleChange>,
    ): List<CalendarSchedule> {
        if (changes.isEmpty()) {
            return this
        }

        return mapNotNull { schedule ->
            when (val change = changes[schedule.id]) {
                LocalScheduleChange.Removed -> null
                is LocalScheduleChange.Updated -> change.schedule
                null -> schedule
            }
        }
    }

    private fun List<CalendarSchedule>.toCountByDate(): Map<LocalDate, Int> =
        mapNotNull { schedule -> schedule.date?.let { date -> date to schedule } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, schedules) -> schedules.size }

    private data class NavigationState(
        val visibleMonth: YearMonth,
        val selectedDate: LocalDate,
        val showYearPicker: Boolean,
        val isEditMode: Boolean,
        val scheduleToDelete: CalendarSchedule?,
    )

    private data class DeviceScheduleState(
        val counts: Map<LocalDate, Int>,
        val schedules: List<CalendarSchedule>,
    )

    private sealed interface LocalScheduleChange {
        data object Removed : LocalScheduleChange

        data class Updated(
            val schedule: CalendarSchedule,
        ) : LocalScheduleChange
    }
}
