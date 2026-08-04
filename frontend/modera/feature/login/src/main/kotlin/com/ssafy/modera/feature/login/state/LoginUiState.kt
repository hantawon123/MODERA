package com.ssafy.modera.feature.login.state

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val type: LoginError) : LoginUiState
}

enum class LoginError {
    KakaoLoginFailed,
    ServerLoginFailed,
}
