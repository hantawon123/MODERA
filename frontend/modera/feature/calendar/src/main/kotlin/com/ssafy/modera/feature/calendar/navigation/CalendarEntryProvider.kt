package com.ssafy.modera.feature.calendar.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import com.ssafy.modera.feature.calendar.CalendarRoute
import java.time.LocalDate

fun EntryProviderScope<NavKey>.calendarEntry(
    onBackClick: () -> Unit,
    onScheduleClick: (CalendarSchedule) -> Unit,
) {
    entry<CalendarNavKey> { key ->
        val selectedDate = key.selectedDate?.let { dateString ->
            runCatching { LocalDate.parse(dateString) }.getOrNull()
        }

        CalendarRoute(
            selectedDate = selectedDate,
            onBackClick = onBackClick,
            onScheduleClick = onScheduleClick,
        )
    }
}
