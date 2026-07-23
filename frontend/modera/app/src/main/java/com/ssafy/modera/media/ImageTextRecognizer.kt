package com.ssafy.modera.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit Text Recognition으로 이미지에서 텍스트를 추출한다.
 *
 * 라틴·한글·중문·일문·데바나가리 스크립트 모델을 병렬로 돌린 뒤
 * 라인 단위로 합친다.
 */
class ImageTextRecognizer : AutoCloseable {

    private val recognizers: List<TextRecognizer> = listOf(
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()),
    )

    /**
     * Content/파일 [Uri] 이미지에서 텍스트를 추출한다.
     *
     * @throws IOException 이미지를 열 수 없을 때
     */
    suspend fun recognize(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return recognize(image)
    }

    /**
     * [SelectedImage]의 Uri에서 텍스트를 추출한다.
     *
     * @throws IOException 이미지를 열 수 없을 때
     */
    suspend fun recognize(context: Context, selectedImage: SelectedImage): String =
        recognize(context, selectedImage.uri)

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

    override fun close() {
        recognizers.forEach(TextRecognizer::close)
    }
}

/**
 * 스크립트별 인식 결과를 라인 단위로 합치고 중복을 제거한다.
 * (CJK/데바나가리 모델도 라틴을 포함해 겹칠 수 있음)
 */
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
