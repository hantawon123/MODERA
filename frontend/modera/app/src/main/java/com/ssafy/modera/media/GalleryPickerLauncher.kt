package com.ssafy.modera.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ssafy.modera.core.model.image.SelectedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 시스템 Photo Picker로 이미지 1장 또는 여러 장을 선택한다.
 *
 * @param maxItems 선택 가능 최대 장수 (1 이상)
 * @param onImagesPicked 선택 완료 콜백. [SelectedImage.originalFileName],
 * [SelectedImage.fileSizeBytes]는 이후 S3 업로드용으로 채워진다.
 */
@Composable
fun rememberGalleryPickerLauncher(
    maxItems: Int = DEFAULT_MAX_IMAGE_COUNT,
    onImagesPicked: (List<SelectedImage>) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val singleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val image = withContext(Dispatchers.IO) {
                uri.toSelectedImage(context.contentResolver)
            }
            onImagesPicked(listOf(image))
        }
    }

    val multipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems.coerceAtLeast(2),
        ),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            val images = withContext(Dispatchers.IO) {
                uris.map { it.toSelectedImage(context.contentResolver) }
            }
            onImagesPicked(images)
        }
    }

    return remember(maxItems, singleLauncher, multipleLauncher) {
        {
            val request = PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly,
            )
            if (maxItems <= 1) {
                singleLauncher.launch(request)
            } else {
                multipleLauncher.launch(request)
            }
        }
    }
}

const val DEFAULT_MAX_IMAGE_COUNT = 10
