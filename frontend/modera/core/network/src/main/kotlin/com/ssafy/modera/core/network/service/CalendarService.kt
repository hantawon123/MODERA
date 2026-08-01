package com.ssafy.modera.core.network.service

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.calendar.SchedulesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CalendarService {

    @GET("api/v1/schedules")
    suspend fun fetchSchedules(
        @Query("calendared") calendared: Boolean?,
        @Query("from") from: String?,
        @Query("to") to: String?,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("sort") sort: String,
    ): ApiResponse<BaseResponse<SchedulesResponse>>
}
