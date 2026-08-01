package com.ssafy.modera.core.network.model.calendar

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleResponse(
    val scheduleId: Long,
    val imageId: Long,
    val title: String,
    val startAt: String? = null,
    val endAt: String? = null,
    val calendared: Boolean,
    val updatedAt: String,
)
