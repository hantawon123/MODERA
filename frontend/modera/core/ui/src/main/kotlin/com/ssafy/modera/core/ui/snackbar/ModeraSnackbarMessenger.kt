package com.ssafy.modera.core.ui.snackbar

import androidx.annotation.DrawableRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

interface ModeraSnackbarMessagesProvider {
    val snackbarMessages: SharedFlow<ModeraSnackbarMessage>
}

class ModeraSnackbarMessenger : ModeraSnackbarMessagesProvider {
    private val _snackbarMessages =
        MutableSharedFlow<ModeraSnackbarMessage>(extraBufferCapacity = 1)

    override val snackbarMessages: SharedFlow<ModeraSnackbarMessage> =
        _snackbarMessages.asSharedFlow()

    suspend fun send(message: ModeraSnackbarMessage) {
        _snackbarMessages.emit(message)
    }

    fun send(
        scope: CoroutineScope,
        message: ModeraSnackbarMessage,
    ) {
        scope.launch {
            send(message)
        }
    }

    fun send(
        scope: CoroutineScope,
        message: String,
        @DrawableRes iconRes: Int? = null,
    ) {
        send(
            scope = scope,
            message = ModeraSnackbarMessage(
                message = message,
                iconRes = iconRes,
            ),
        )
    }
}
