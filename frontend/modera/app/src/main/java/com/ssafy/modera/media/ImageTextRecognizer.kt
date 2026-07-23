package com.ssafy.modera.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ssafy.modera.core.model.image.SelectedImage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.core.net.toUri

/**
 * ML Kit Text Recognition으로 이미지에서 텍스트를 추출한다.
 */
class ImageTextRecognizer : AutoCloseable {

    private val recognizers: List<TextRecognizer> = listOf(
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
    )

    /**
     * Content/파일 [Uri] 이미지에서 텍스트를 추출한다.
     */
    suspend fun recognize(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return recognize(image)
    }

    /**
     * [SelectedImage]의 uri 문자열에서 텍스트를 추출한다.
     */
    suspend fun recognize(context: Context, selectedImage: SelectedImage): String =
        recognize(context, selectedImage.uri.toUri())

    /**
     * [Bitmap]에서 텍스트를 추출한다.
     *
     * @param rotationDegrees 이미지 회전 각도 (0, 90, 180, 270)
     */
    suspend fun recognize(bitmap: Bitmap, rotationDegrees: Int = 0): String {
        val image = InputImage.fromBitmap(bitmap, rotationDegrees)
        return recognize(image)
    }

    /**
     * [InputImage]에서 인식된 전체 텍스트를 반환한다.
     */
    suspend fun recognize(image: InputImage): String = coroutineScope {
        recognizers
            .map { recognizer ->
                async {
                    runCatching { recognizer.process(image).await().text }
                        .getOrDefault("")
                }
            }
            .awaitAll()
            .let(::mergeRecognizedTexts)
    }

    /**
     * 여러 이미지에 대해 OCR을 수행하고, 각 [SelectedImage]에 추출 텍스트를 매핑한다.
     * 한 장이 실패해도 나머지는 계속 처리한다.
     */
    suspend fun recognizeAll(
        context: Context,
        images: List<SelectedImage>,
    ): List<SelectedImage> =
        images.map { image ->
            val text = runCatching { recognize(context, image) }.getOrDefault("")
            image.copy(ocrText = text)
        }

    override fun close() {
        recognizers.forEach(TextRecognizer::close)
    }
}

private fun mergeRecognizedTexts(texts: List<String>): String =
    texts.asSequence()
        .flatMap { it.lineSequence() }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString("\n")

private suspend fun <T> Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) {
                continuation.resumeWithException(exception)
            }
        }
    }
