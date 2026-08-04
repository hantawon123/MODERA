package com.ssafy.modera.session

sealed interface AppSessionUiState {
    data object Loading : AppSessionUiState
    data object Authenticated : AppSessionUiState
    data object Unauthenticated : AppSessionUiState
}
