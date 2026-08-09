package com.ssafy.modera.feature.calendar.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class CalendarNavKey(
    val selectedDate: String? = null,
) : NavKey

fun Navigator.navigateToCalendar(
    selectedDate: LocalDate? = null,
) {
    navigate(
        CalendarNavKey(
            selectedDate = selectedDate?.toString(),
        ),
    )
}
