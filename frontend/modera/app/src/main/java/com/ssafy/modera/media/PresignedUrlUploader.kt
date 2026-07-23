package com.ssafy.modera.media

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

/**
 * 등록 API가 내려준 presigned [uploadUrl]로 원본 이미지를 PUT 업로드한다.
 *
 * presigned URL이 Content-Type을 서명에 포함한 경우 그 값을 그대로 쓰고,
 * 서명에 Content-Type이 없으면 Content-Type 헤더를 붙이지 않는다.
 */
@Singleton
class PresignedUrlUploader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    @param:ApplicationContext private val context: Context,
) {
    private val uploadClient: OkHttpClient by lazy {
        okHttpClient.newBuilder().apply {
            interceptors().clear()
            networkInterceptors().clear()
        }.build()
    }

    suspend fun upload(
        localUri: String,
        uploadUrl: String,
    ) = withContext(Dispatchers.IO) {
        val uri = localUri.toUri()
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: error("이미지를 열 수 없습니다: $localUri")

        check(bytes.isNotEmpty()) { "업로드할 이미지 바이트가 비어 있습니다: $localUri" }

        val signedContentType = contentTypeFromUploadUrl(uploadUrl)
        val resolverType = context.contentResolver.getType(uri)
        val mediaType = when {
            signedContentType != null -> signedContentType.toMediaType()
            else -> null
        }

        Log.d(
            LOG_TAG,
            "PUT 업로드 준비 bytes=${bytes.size}, " +
                "signedContentType=$signedContentType, resolverType=$resolverType, " +
                "sendContentType=${mediaType?.toString() ?: "(none)"}",
        )

        val requestBody = bytes.toUploadRequestBody(mediaType)
        val request = Request.Builder()
            .url(uploadUrl)
            .put(requestBody)
            .build()

        uploadClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            Log.d(
                LOG_TAG,
                "PUT 응답 code=${response.code}, body=${responseBody.take(300)}",
            )
            if (!response.isSuccessful) {
                error("스토리지 업로드 실패: HTTP ${response.code} $responseBody")
            }
        }
    }

    private fun contentTypeFromUploadUrl(uploadUrl: String): String? {
        val httpUrl = uploadUrl.toHttpUrlOrNull() ?: return null
        return httpUrl.queryParameter("Content-Type")
            ?: httpUrl.queryParameter("content-type")
    }

    private fun ByteArray.toUploadRequestBody(mediaType: MediaType?): RequestBody =
        object : RequestBody() {
            override fun contentType(): MediaType? = mediaType

            override fun contentLength(): Long = size.toLong()

            override fun writeTo(sink: BufferedSink) {
                sink.write(this@toUploadRequestBody)
            }
        }

    private companion object {
        const val LOG_TAG = "MODERA_S3_UPLOAD"
    }
}
