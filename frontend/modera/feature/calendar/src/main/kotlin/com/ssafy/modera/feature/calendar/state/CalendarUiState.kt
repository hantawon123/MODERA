package com.ssafy.modera.feature.calendar.state

import com.ssafy.modera.core.model.calendar.CalendarSchedule
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val appScheduleCountByDate: Map<LocalDate, Int>,
    val deviceScheduleCountByDate: Map<LocalDate, Int>,
    val schedules: List<CalendarSchedule>,
    val showYearPicker: Boolean,
    val isEditMode: Boolean = false,
    val scheduleToDelete: CalendarSchedule? = null,
)
