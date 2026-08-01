package com.ssafy.modera.core.model.calendar

import java.time.LocalDate
import java.time.LocalTime

data class CalendarSchedule(
    val id: Long,
    val title: String,
    val source: CalendarScheduleSource,
    val date: LocalDate? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val isAdded: Boolean = false,
)

fun List<CalendarSchedule>.addedSchedules(): List<CalendarSchedule> =
    filter { schedule ->
        schedule.source == CalendarScheduleSource.DEVICE || schedule.isAdded
    }.sortedForDay()

fun List<CalendarSchedule>.pendingSchedules(): List<CalendarSchedule> =
    filter { schedule ->
        schedule.source == CalendarScheduleSource.APP && !schedule.isAdded
    }.sortedForDay()

fun List<CalendarSchedule>.editableSchedules(): List<CalendarSchedule> =
    filter { schedule ->
        schedule.source == CalendarScheduleSource.APP && schedule.isAdded
    }.sortedForDay()

fun List<CalendarSchedule>.sortedForDay(): List<CalendarSchedule> =
    sortedWith { left, right ->
        val leftTimed = left.startTime != null
        val rightTimed = right.startTime != null

        when {
            leftTimed && rightTimed -> {
                compareValuesBy(
                    left,
                    right,
                    { it.startTime },
                    { it.endTime },
                    { it.id },
                )
            }

            leftTimed -> -1
            rightTimed -> 1
            else -> {
                compareValuesBy(
                    left,
                    right,
                    { it.source != CalendarScheduleSource.APP },
                    { it.id },
                )
            }
        }
    }
