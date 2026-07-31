package com.ssafy.modera.feature.calendar.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
object CalendarNavKey : NavKey

fun Navigator.navigateToCalendar() {
    navigate(CalendarNavKey)
}
