package com.ssafy.modera.core.network.model.calendar

import kotlinx.serialization.Serializable

@Serializable
data class UpdateScheduleCalendarRequest(
    val calendared: Boolean,
)