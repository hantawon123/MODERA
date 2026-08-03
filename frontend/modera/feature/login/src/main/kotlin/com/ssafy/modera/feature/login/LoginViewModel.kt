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
            }.onFailure {
                _uiState.value = LoginUiState.Error(
                    message = "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.",
                )
            }
        }
    }

    fun showKakaoLoginError() {
        _uiState.value = LoginUiState.Error(
            message = "카카오 로그인을 완료하지 못했습니다.",
        )
    }
}
