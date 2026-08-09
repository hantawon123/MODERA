package com.ssafy.modera.feature.settings

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Success(
        val email: String,
        val appVersion: String,
    ) : SettingsUiState
}
