package com.ssafy.modera.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.data.repository.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun loginWithKakao(kakaoAccessToken: String) {
        if (_uiState.value == LoginUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            runCatching {
                authRepository.loginWithKakao(kakaoAccessToken)
            }.onSuccess {
                _uiState.value = LoginUiState.Idle
            }.onFailure {
                _uiState.value = LoginUiState.Error(LoginError.ServerLoginFailed)
            }
        }
    }

    fun showKakaoLoginError() {
        _uiState.value = LoginUiState.Error(LoginError.KakaoLoginFailed)
    }

    fun resetUiState() {
        if (_uiState.value == LoginUiState.Loading) {
            _uiState.value = LoginUiState.Idle
        }
    }
}
