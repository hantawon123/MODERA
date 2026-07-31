package com.ssafy.modera.feature.calendar.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.feature.calendar.CalendarRoute

fun EntryProviderScope<NavKey>.calendarEntry(
    onBackClick: () -> Unit,
) {
    entry<CalendarNavKey> {
        CalendarRoute(
            onBackClick = onBackClick,
        )
    }
}
