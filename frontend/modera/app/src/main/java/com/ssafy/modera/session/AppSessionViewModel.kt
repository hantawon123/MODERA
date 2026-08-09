package com.ssafy.modera.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.modera.core.data.repository.analyzedImage.AnalyzedImageRepository
import com.ssafy.modera.core.data.repository.auth.AuthRepository
import com.ssafy.modera.core.data.repository.document.DocumentRepository
import com.ssafy.modera.core.ui.snackbar.ModeraSnackbarMessage
import com.ssafy.modera.core.ui.snackbar.ModeraSnackbarMessagesProvider
import com.ssafy.modera.core.ui.snackbar.ModeraSnackbarMessenger
import com.ssafy.modera.feature.login.util.logoutFromKakao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val analyzedImageRepository: AnalyzedImageRepository,
    private val documentRepository: DocumentRepository,
) : ViewModel(), ModeraSnackbarMessagesProvider {
    private val isRestoring = MutableStateFlow(true)
    private val snackbarMessenger = ModeraSnackbarMessenger()

    override val snackbarMessages = snackbarMessenger.snackbarMessages

    val uiState: StateFlow<AppSessionUiState> = combine(
        authRepository.session,
        isRestoring,
    ) { session, restoring ->
        when {
            restoring -> AppSessionUiState.Loading
            session.isAuthenticated -> AppSessionUiState.Authenticated
            else -> AppSessionUiState.Unauthenticated
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSessionUiState.Loading,
    )

    init {
        viewModelScope.launch {
            try {
                authRepository.restoreSession()
            } finally {
                isRestoring.value = false
            }
        }

        viewModelScope.launch {
            authRepository.session
                .map { session -> session.isAuthenticated }
                .distinctUntilChanged()
                .filter { authenticated -> authenticated }
                .collect {
                    runCatching {
                        analyzedImageRepository.refreshAnalyzedImagesIfEmpty()
                    }
                    runCatching {
                        documentRepository.refreshDocumentsIfEmpty()
                    }
                }
        }
    }

    fun logout(snackbarMessage: ModeraSnackbarMessage) {
        logoutFromKakao()
        viewModelScope.launch {
            authRepository.logout()
            snackbarMessenger.send(snackbarMessage)
        }
    }
}
