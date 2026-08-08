package com.ssafy.modera.feature.analyzedimage.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.result.Result
import com.ssafy.modera.core.common.result.asResult
import com.ssafy.modera.core.data.repository.analyzedImage.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.calendar.CalendarRepository
import com.ssafy.modera.core.model.calendar.CalendarSchedule
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

@HiltViewModel(
    assistedFactory = AnalyzedImageDetailViewModel.Factory::class,
)
class AnalyzedImageDetailViewModel @AssistedInject constructor(
    private val analyzedImageRepository: AnalyzedImageRepository,
    private val calendarRepository: CalendarRepository,
    @Assisted private val imageId: Long,
) : ViewModel() {

    val uiState: StateFlow<AnalyzedImageDetailUiState>
        field = MutableStateFlow<AnalyzedImageDetailUiState>(
            AnalyzedImageDetailUiState.Loading,
        )

    private val _relatedSchedulesDialog = MutableStateFlow<List<CalendarSchedule>?>(null)
    val relatedSchedulesDialog: StateFlow<List<CalendarSchedule>?> = _relatedSchedulesDialog

    private val navigateToCalendarChannel = Channel<LocalDate>(Channel.BUFFERED)
    val navigateToCalendarDate = navigateToCalendarChannel.receiveAsFlow()

    private var isRelatedScheduleLoading = false

    init {
        loadAnalyzedImageDetail()
    }

    private fun loadAnalyzedImageDetail() {
        viewModelScope.launch {
            analyzedImageRepository
                .getAnalyzedImageDetail(imageId)
                .asResult()
                .collect { result ->
                    uiState.value = when (result) {
                        Result.Loading -> {
                            AnalyzedImageDetailUiState.Loading
                        }

                        is Result.Success -> {
                            AnalyzedImageDetailUiState.Success(
                                image = result.data,
                            )
                        }

                        is Result.Error -> {
                            Log.d("testaaa", "${result.exception.message}")
                            AnalyzedImageDetailUiState.Error(
                                exception = result.exception,
                            )
                        }
                    }
                }
        }
    }

    fun reanalyzeImage() {
        if (uiState.value !is AnalyzedImageDetailUiState.Success) {
            return
        }

        uiState.value = AnalyzedImageDetailUiState.Reanalyzing

        viewModelScope.launch {
            analyzedImageRepository
                .reanalyzeImage(
                    imageId = imageId,
                )
                .flatMapLatest {
                    analyzedImageRepository.getAnalyzedImageDetail(
                        imageId = imageId,
                    )
                }
                .asResult()
                .collect { result ->
                    uiState.value = when (result) {
                        Result.Loading -> {
                            AnalyzedImageDetailUiState.Reanalyzing
                        }

                        is Result.Success -> {
                            AnalyzedImageDetailUiState.Success(
                                image = result.data,
                            )
                        }

                        is Result.Error -> {
                            AnalyzedImageDetailUiState.Error(
                                exception = result.exception,
                            )
                        }
                    }
                }
        }
    }

    fun deleteAnalyzedImage(
        onDeleted: () -> Unit,
    ) {
        viewModelScope.launch {
            analyzedImageRepository
                .deleteAnalyzedImage(
                    imageId = imageId,
                )
                .asResult()
                .collect { result ->
                    when (result) {
                        Result.Loading -> {
                            uiState.value = AnalyzedImageDetailUiState.Loading
                        }

                        is Result.Success -> {
                            onDeleted()
                        }

                        is Result.Error -> {
                            uiState.value =
                                AnalyzedImageDetailUiState.Error(
                                    exception = result.exception,
                                )
                        }
                    }
                }
        }
    }

    private var isFavoriteUpdating = false

    fun toggleAnalyzedImageFavorite() {
        val currentState =
            uiState.value as? AnalyzedImageDetailUiState.Success
                ?: return

        if (isFavoriteUpdating) return

        val previousImage = currentState.image
        val updatedFavorite = !previousImage.favorite

        // 클릭 즉시 화면에 반영
        uiState.value = currentState.copy(
            image = previousImage.copy(
                favorite = updatedFavorite,
            ),
        )

        isFavoriteUpdating = true

        viewModelScope.launch {
            analyzedImageRepository
                .setAnalyzedImageFavorite(
                    imageId = imageId,
                    favorite = updatedFavorite,
                )
                .asResult()
                .collect { result ->
                    when (result) {
                        Result.Loading -> Unit

                        is Result.Success -> {
                            isFavoriteUpdating = false
                        }

                        is Result.Error -> {
                            isFavoriteUpdating = false

                            // 실패하면 기존 값으로 복구
                            val latestState =
                                uiState.value as? AnalyzedImageDetailUiState.Success

                            if (latestState != null) {
                                uiState.value = latestState.copy(
                                    image = latestState.image.copy(
                                        favorite = previousImage.favorite,
                                    ),
                                )
                            }
                        }
                    }
                }
        }
    }

    fun onRelatedScheduleClick() {
        if (isRelatedScheduleLoading) return

        isRelatedScheduleLoading = true

        viewModelScope.launch {
            calendarRepository
                .getSchedulesByImageId(imageId)
                .asResult()
                .collect { result ->
                    when (result) {
                        Result.Loading -> Unit

                        is Result.Success -> {
                            isRelatedScheduleLoading = false
                            handleRelatedSchedules(result.data)
                        }

                        is Result.Error -> {
                            isRelatedScheduleLoading = false
                        }
                    }
                }
        }
    }

    fun onRelatedScheduleSelected(
        schedule: CalendarSchedule,
    ) {
        schedule.date?.let { date ->
            navigateToCalendar(date)
        }
        dismissRelatedSchedulesDialog()
    }

    fun dismissRelatedSchedulesDialog() {
        _relatedSchedulesDialog.value = null
    }

    private fun handleRelatedSchedules(
        schedules: List<CalendarSchedule>,
    ) {
        val navigableSchedules = schedules.filter { schedule ->
            schedule.date != null
        }

        when {
            navigableSchedules.isEmpty() -> Unit

            navigableSchedules.size == 1 -> {
                navigateToCalendar(navigableSchedules.first().date!!)
            }

            else -> {
                _relatedSchedulesDialog.value = navigableSchedules
            }
        }
    }

    private fun navigateToCalendar(
        date: LocalDate,
    ) {
        viewModelScope.launch {
            navigateToCalendarChannel.send(date)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            imageId: Long,
        ): AnalyzedImageDetailViewModel
    }
}