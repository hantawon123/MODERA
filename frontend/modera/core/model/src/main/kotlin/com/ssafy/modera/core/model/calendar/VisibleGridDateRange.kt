package com.ssafy.modera.core.model.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class LocalDateRange(
    val start: LocalDate,
    val endInclusive: LocalDate,
)

fun YearMonth.toVisibleGridDates(): List<LocalDate> {
    val firstOfMonth = atDay(1)
    val sundayOffset = firstOfMonth.dayOfWeek.value % DayOfWeek.SUNDAY.value
    val startDate = firstOfMonth.minusDays(sundayOffset.toLong())
    val daysNeeded = sundayOffset + lengthOfMonth()
    val weekCount = (daysNeeded + 6) / 7
    return List(weekCount * 7) { index -> startDate.plusDays(index.toLong()) }
}

fun YearMonth.toVisibleGridDateRange(): LocalDateRange {
    val dates = toVisibleGridDates()
    return LocalDateRange(
        start = dates.first(),
        endInclusive = dates.last(),
    )
}
