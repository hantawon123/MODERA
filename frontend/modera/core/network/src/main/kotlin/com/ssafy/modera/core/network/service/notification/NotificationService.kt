package com.ssafy.modera.core.network.service.notification

import com.skydoves.sandwich.ApiResponse
import com.ssafy.modera.core.network.model.BaseResponse
import com.ssafy.modera.core.network.model.notification.UpdatePushTokenRequest
import com.ssafy.modera.core.network.model.notification.UpdatePushTokenResponse
import retrofit2.http.Body
import retrofit2.http.PUT

interface NotificationService {
    @PUT("api/v1/user/devices/push-token")
    suspend fun updatePushToken(
        @Body request: UpdatePushTokenRequest,
    ): ApiResponse<BaseResponse<UpdatePushTokenResponse>>
}
