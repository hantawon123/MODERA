package com.ssafy.modera.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.ssafy.modera.R

object FcmNotificationChannel {
    const val DEFAULT_CHANNEL_ID = "modera_default"

    fun create(context: Context) {

        val notificationManager = context.getSystemService<NotificationManager>() ?: return

        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            context.getString(R.string.fcm_default_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(
                R.string.fcm_default_notification_channel_description,
            )
        }

        notificationManager.createNotificationChannel(channel)
    }
}
