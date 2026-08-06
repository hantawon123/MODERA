package com.ssafy.modera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.data.repository.CategoryRepository
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.model.image.SelectedImage
import com.ssafy.modera.core.ui.snackbar.ModeraSnackbarMessage
import com.ssafy.modera.core.ui.snackbar.ModeraSnackbarMessagesProvider
import com.ssafy.modera.core.ui.snackbar.ModeraSnackbarMessenger
import com.ssafy.modera.media.ImageTextRecognizer
import com.ssafy.modera.registration.ImageRegisterLogger
import com.ssafy.modera.registration.ImageRegistrationHandler
import com.ssafy.modera.registration.MainUiState
import com.ssafy.modera.registration.applyRegistrationOutcome
import com.ssafy.modera.registration.startRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val imageRegistrationHandler: ImageRegistrationHandler,
    private val categoryRepository: CategoryRepository,
) : ViewModel(), ModeraSnackbarMessagesProvider {
    private val snackbarMessenger = ModeraSnackbarMessenger()

    override val snackbarMessages = snackbarMessenger.snackbarMessages

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var registerJob: Job? = null

    fun onImagesPicked(images: List<SelectedImage>) {
        if (images.isEmpty()) return

        registerJob?.cancel()
        _uiState.update { it.startRegistration(images.size) }

        registerJob = viewModelScope.launch {
            ImageRegisterLogger.logRegistrationStart(images.size)

            ImageTextRecognizer().use { ocr ->
                images.forEachIndexed { index, image ->
                    registerSingleImage(ocr, image, index)
                }
            }

            notifyRegisterSummary(totalCount = images.size)

            if (_uiState.value.registeredImages.isNotEmpty()) {
                categoryRepository.refreshCategories()
            }
        }
    }

    private suspend fun registerSingleImage(
        ocr: ImageTextRecognizer,
        image: SelectedImage,
        index: Int,
    ) {
        val outcome = imageRegistrationHandler.registerImage(
            ocr = ocr,
            image = image,
            index = index,
        )
        _uiState.update { it.applyRegistrationOutcome(outcome) }
    }

    private suspend fun notifyRegisterSummary(totalCount: Int) {
        val state = _uiState.value

        ImageRegisterLogger.logRegistrationSummary(
            totalCount = totalCount,
            registeredCount = state.registeredImages.size,
            duplicatedCount = state.duplicatedImages.size,
            failedCount = state.failedImages.size,
        )

        if (totalCount <= 0) return

        state.duplicatedSummaryMessage()?.let { message ->
            snackbarMessenger.send(
                ModeraSnackbarMessage(
                    message = message,
                    iconRes = ModeraIcons.Images,
                ),
            )
        }
        state.failedSummaryMessage()?.let { message ->
            snackbarMessenger.send(
                ModeraSnackbarMessage(
                    message = message,
                    iconRes = ModeraIcons.Images,
                ),
            )
        }
    }
}
