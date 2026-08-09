package com.ssafy.modera.core.network.model.calendar

import kotlinx.serialization.Serializable

@Serializable
data class DeleteScheduleResponse(
    val scheduleId: Long,
    val deleted: Boolean,
)
