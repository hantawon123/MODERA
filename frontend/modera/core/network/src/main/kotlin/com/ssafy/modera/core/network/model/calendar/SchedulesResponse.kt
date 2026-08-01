package com.ssafy.modera.core.network.model.calendar

import kotlinx.serialization.Serializable

@Serializable
data class SchedulesResponse(
    val list: List<ScheduleResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
)
