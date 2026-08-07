package com.ssafy.modera.media

import android.content.Intent

/**
 * 갤러리/공유 시트에서 전달된 이미지 Uri 목록을 추출한다.
 * [Intent.ACTION_SEND], [Intent.ACTION_SEND_MULTIPLE] + `image/*` 만 처리한다.
 */
fun Intent.extractSharedImageUris(
    maxItems: Int = DEFAULT_MAX_IMAGE_COUNT,
): List<Uri> {
    val mimeType = type ?: return emptyList()
    if (!mimeType.startsWith("image/")) return emptyList()

    val uris = when (action) {
        Intent.ACTION_SEND -> {
            listOfNotNull(
                IntentCompat.getParcelableExtra(
                    this,
                    Intent.EXTRA_STREAM,
                    Uri::class.java,
                ),
            )
        }

        Intent.ACTION_SEND_MULTIPLE -> {
            IntentCompat.getParcelableArrayListExtra(
                this,
                Intent.EXTRA_STREAM,
                Uri::class.java,
            ).orEmpty()
        }

        else -> emptyList()
    }

    return uris
        .filterNot { uri -> uri == Uri.EMPTY }
        .distinct()
        .take(maxItems.coerceAtLeast(1))
}
