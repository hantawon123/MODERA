package com.ssafy.modera.media

import android.content.ContentResolver
import android.net.Uri
import java.security.MessageDigest

/**
 * Content/[Uri] 바이트 스트림의 SHA-256 해시(hex 소문자 64자)를 계산한다.
 */
fun Uri.sha256(contentResolver: ContentResolver): String {
    val digest = MessageDigest.getInstance("SHA-256")
    contentResolver.openInputStream(this)?.use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    } ?: error("이미지를 열 수 없습니다: $this")

    return digest.digest().joinToString("") { byte ->
        "%02x".format(byte)
    }
}
