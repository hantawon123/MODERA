package com.ssafy.modera

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ModeraApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoSdk.init(
                context = this,
                appKey = BuildConfig.KAKAO_NATIVE_APP_KEY,
            )
        }
    }
}
