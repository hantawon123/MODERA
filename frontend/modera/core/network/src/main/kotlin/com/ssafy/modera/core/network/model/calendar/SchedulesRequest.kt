package com.ssafy.modera.core.network.model.calendar

data class SchedulesRequest(
    val calendared: Boolean? = null,
    val from: String? = null,
    val to: String? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sort: ScheduleSortType = ScheduleSortType.START_ASC,
)

enum class ScheduleSortType(
    val queryValue: String,
) {
    START_ASC(queryValue = "START_ASC"),
    START_DESC(queryValue = "START_DESC"),
}
