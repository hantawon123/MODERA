package com.ssafy.modera.fcm

import android.content.Intent
import com.ssafy.modera.sync.work.SyncResourceType

sealed interface NotificationNavigationTarget {

    data class ImageDetail(
        val imageId: Long,
    ) : NotificationNavigationTarget

    data class DocumentDetail(
        val documentId: Long,
    ) : NotificationNavigationTarget
}

internal fun Intent.toNotificationNavigationTarget():
        NotificationNavigationTarget? {
    val resourceId = getLongExtra(
        FcmNotificationManager.EXTRA_RESOURCE_ID,
        INVALID_RESOURCE_ID,
    ).takeIf { id ->
        id > 0L
    } ?: return null

    return when (
        getStringExtra(FcmNotificationManager.EXTRA_RESOURCE)
    ) {
        SyncResourceType.IMAGE.name -> {
            NotificationNavigationTarget.ImageDetail(
                imageId = resourceId,
            )
        }

        SyncResourceType.DOCUMENT.name -> {
            NotificationNavigationTarget.DocumentDetail(
                documentId = resourceId,
            )
        }

        else -> {
            null
        }
    }
}

private const val INVALID_RESOURCE_ID = -1L