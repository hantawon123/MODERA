package com.ssafy.modera

import android.app.Application
import com.ssafy.modera.fcm.FcmNotificationChannel
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ModeraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FcmNotificationChannel.create(this)
    }
}