package com.ssafy.modera.core.data.repository.notification

interface PushTokenRepository {

    /**
     * Firebase에서 새 FID를 전달받았을 때 호출합니다.
     *
     * FID를 로컬에 저장하고, 인증 상태라면 서버에도 등록합니다.
     *
     * @return 서버 등록까지 완료됐는지 여부
     */
    suspend fun registerPushToken(
        installationId: String,
    ): Boolean

    /**
     * 로컬에 저장된 FID를 현재 로그인 세션의 deviceId와 함께
     * 서버에 등록합니다.
     */
    suspend fun syncPushToken(): Boolean

    suspend fun deletePushToken()
}