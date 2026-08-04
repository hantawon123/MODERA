package com.ssafy.modera

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kakao.sdk.common.KakaoSdk
import com.ssafy.modera.fcm.FcmNotificationChannel
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ModeraApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        FcmNotificationChannel.create(this)

        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoSdk.init(
                context = this,
                appKey = BuildConfig.KAKAO_NATIVE_APP_KEY,
            )
        }
    }
}