package com.ssafy.modera.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.common.sync.SyncCompletionNotifier
import com.ssafy.modera.feature.onboarding.impl.model.OnboardingAnalysisState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    syncCompletionNotifier: SyncCompletionNotifier,
) : ViewModel() {

    val analysisState: StateFlow<OnboardingAnalysisState>
        field = MutableStateFlow(OnboardingAnalysisState.Idle)

    private var isWaitingForAnalysis = false

    init {
        viewModelScope.launch {
            syncCompletionNotifier
                .events
                .collect {
                    if (!isWaitingForAnalysis) {
                        return@collect
                    }

                    isWaitingForAnalysis = false

                    analysisState.value =
                        OnboardingAnalysisState.Completed
                }
        }
    }

    fun onAnalysisStarted() {
        isWaitingForAnalysis = true

        analysisState.value = OnboardingAnalysisState.Analyzing
    }
}