package com.ssafy.modera.media

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.ssafy.modera.core.model.image.SelectedImage

private const val FALLBACK_FILE_NAME = "image"
private const val UNKNOWN_FILE_SIZE_BYTES = -1L

/**
 * Content Uri에서 원본 파일명·파일 크기(byte)를 읽어 [SelectedImage]로 변환한다.
 * Photo Picker / 갤러리 선택 결과에 사용한다.
 */
fun Uri.toSelectedImage(contentResolver: ContentResolver): SelectedImage {
    var originalFileName = FALLBACK_FILE_NAME
    var fileSizeBytes = UNKNOWN_FILE_SIZE_BYTES

    contentResolver.query(
        this,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use

        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
            originalFileName = cursor.getString(nameIndex)
        }

        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
            fileSizeBytes = cursor.getLong(sizeIndex)
        }
    }

    // OpenableColumns.SIZE가 비어 있는 경우 스트림 길이로 보완
    if (fileSizeBytes < 0) {
        fileSizeBytes = contentResolver.openAssetFileDescriptor(this, "r")
            ?.use { it.length }
            ?.takeIf { it >= 0 }
            ?: UNKNOWN_FILE_SIZE_BYTES
    }

    return SelectedImage(
        uri = toString(),
        originalFileName = originalFileName,
        fileSizeBytes = fileSizeBytes,
    )
}
