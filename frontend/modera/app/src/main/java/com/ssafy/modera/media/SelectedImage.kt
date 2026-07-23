package com.ssafy.modera.media

import android.net.Uri

data class SelectedImage(
    val uri: Uri,
    val originalFileName: String,
    val fileSizeBytes: Long,
)
