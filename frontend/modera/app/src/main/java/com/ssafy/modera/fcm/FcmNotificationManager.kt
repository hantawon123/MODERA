package com.ssafy.modera.fcm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ssafy.modera.R
import com.ssafy.modera.sync.work.SyncResourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    @SuppressLint("MissingPermission")
    fun show(event: DataChangedEvent) {
        createNotificationChannel()

        if (!hasNotificationPermission()) {
            return
        }

        val contentIntent = createContentIntent(
            event = event,
        )

        val content = event.toNotificationContent()

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content.body),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(
                NotificationCompat.DEFAULT_SOUND or
                        NotificationCompat.DEFAULT_VIBRATE,
            )
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .apply {
                contentIntent?.let(::setContentIntent)
            }
            .build()

        NotificationManagerCompat
            .from(context)
            .notify(
                event.eventId.hashCode(),
                notification,
            )
    }

    private fun createContentIntent(
        event: DataChangedEvent,
    ): PendingIntent? {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                addFlags(
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )

                putExtra(
                    EXTRA_RESOURCE,
                    event.resource.name,
                )
                putExtra(
                    EXTRA_RESOURCE_ID,
                    event.resourceId,
                )
            }
            ?: return null

        return PendingIntent.getActivity(
            context,
            event.eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESCRIPTION
        }

        context
            .getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED

    private fun DataChangedEvent.toNotificationContent(): NotificationContent =
        when (resource) {
            SyncResourceType.IMAGE_UPLOAD -> {
                NotificationContent(
                    title = "이미지 분석 완료",
                    body = "눌러서 분석 결과를 확인해 보세요.",
                )
            }

            SyncResourceType.IMAGE_CATEGORY -> {
                NotificationContent(
                    title = "이미지 재분류 완료",
                    body = "눌러서 분석 결과를 확인해 보세요.",
                )
            }

            SyncResourceType.DOCUMENT_UPLOAD -> {
                NotificationContent(
                    title = "문서 생성 완료",
                    body = "새로운 문서 내용을 확인해 보세요.",
                )
            }

            SyncResourceType.DOCUMENT_REANALYSIS -> {
                NotificationContent(
                    title = "문서 재생성 완료",
                    body = "새로운 문서 내용을 확인해 보세요.",
                )
            }

            SyncResourceType.CALENDAR -> {
                NotificationContent(
                    title = "일정 등록 완료",
                    body = "등록된 일정 내용을 확인해 보세요.",
                )
            }
        }

    private data class NotificationContent(
        val title: String,
        val body: String,
    )

    companion object {
        const val EXTRA_RESOURCE = "fcm_resource"
        const val EXTRA_RESOURCE_ID = "fcm_resource_id"

        private const val CHANNEL_ID = "data_updates_high"
        private const val CHANNEL_NAME = "분석 결과"
        private const val CHANNEL_DESCRIPTION =
            "이미지 분석, 문서 및 일정 변경 결과를 알려줍니다."
    }
}