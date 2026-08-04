package com.ssafy.modera.fcm

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.modera.R

object FcmMessageToast {
    fun show(
        context: Context,
        message: RemoteMessage,
    ) {
        val text = message.toDisplayText(context) ?: return

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                context.applicationContext,
                text,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun RemoteMessage.toDisplayText(context: Context): String? {
        notification?.let { notification ->
            val parts = listOfNotNull(
                notification.title?.takeIf { it.isNotBlank() },
                notification.body?.takeIf { it.isNotBlank() },
            )
            if (parts.isNotEmpty()) {
                return parts.joinToString("\n")
            }
        }

        data["title"]?.takeIf { it.isNotBlank() }?.let { title ->
            val body = data["body"]?.takeIf { it.isNotBlank() }
                ?: data["message"]?.takeIf { it.isNotBlank() }
            return if (body != null) {
                "$title\n$body"
            } else {
                title
            }
        }

        return data["message"]?.takeIf { it.isNotBlank() }
            ?: data["body"]?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.fcm_default_toast_message)
    }
}
