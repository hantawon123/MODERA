package com.ssafy.modera.core.ui.snackbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.SharedFlow

val LocalModeraSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("LocalModeraSnackbarHostState not provided")
}

@Composable
fun rememberModeraSnackbarHostState(): SnackbarHostState = remember {
    SnackbarHostState()
}

suspend fun SnackbarHostState.showModeraSnackbar(
    message: String,
    @DrawableRes iconRes: Int? = null,
) {
    showSnackbar(
        ModeraSnackbarVisuals(
            message = message,
            iconRes = iconRes,
        ),
    )
}

suspend fun SnackbarHostState.showModeraSnackbar(message: ModeraSnackbarMessage) {
    showSnackbar(message.toVisuals())
}

@Composable
fun ModeraSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.exclude(WindowInsets.ime),
        ),
        snackbar = { data ->
            ModeraSnackbarFromData(data)
        },
    )
}

@Composable
private fun ModeraSnackbarFromData(data: SnackbarData) {
    val iconRes = (data.visuals as? ModeraSnackbarVisuals)?.iconRes
    ModeraSnackbarContent(
        message = data.visuals.message,
        iconRes = iconRes,
    )
}

@Composable
fun ModeraSnackbarProvider(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalModeraSnackbarHostState provides snackbarHostState) {
            content()
        }

        ModeraSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
fun ModeraSnackbarEffect(
    messages: SharedFlow<ModeraSnackbarMessage>,
    snackbarHostState: SnackbarHostState = LocalModeraSnackbarHostState.current,
) {
    LaunchedEffect(messages, snackbarHostState) {
        messages.collect { message ->
            snackbarHostState.showModeraSnackbar(message)
        }
    }
}

@Composable
fun ModeraSnackbarEffect(
    provider: ModeraSnackbarMessagesProvider,
    snackbarHostState: SnackbarHostState = LocalModeraSnackbarHostState.current,
) {
    ModeraSnackbarEffect(
        messages = provider.snackbarMessages,
        snackbarHostState = snackbarHostState,
    )
}
