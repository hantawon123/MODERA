package com.ssafy.modera.core.network.model.calendar

import kotlinx.serialization.Serializable

@Serializable
data class UpdateScheduleCalendarResponse(
    val scheduleId: Long,
    val calendared: Boolean,
    val updatedAt: String,
)
