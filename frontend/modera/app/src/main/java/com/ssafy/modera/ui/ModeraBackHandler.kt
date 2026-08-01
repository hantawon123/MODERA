package com.ssafy.modera.ui

import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ssafy.modera.R
import com.ssafy.modera.core.navigation.Navigator

private const val EXIT_BACK_PRESS_INTERVAL_MILLIS = 2_000L

@Composable
fun rememberModeraBackHandler(
    navigator: Navigator,
): () -> Unit {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val exitMessage = stringResource(R.string.back_press_exit_message)
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    return remember(navigator, context, activity, exitMessage) {
        {
            when {
                navigator.state.currentSubStack.size > 1 -> {
                    navigator.popBackStack()
                }

                !navigator.isAtHomeTabRoot() -> {
                    lastBackPressTime = 0L
                    navigator.navigateToHome()
                }

                else -> {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastBackPressTime <= EXIT_BACK_PRESS_INTERVAL_MILLIS) {
                        activity?.finish()
                    } else {
                        lastBackPressTime = now
                        Toast.makeText(context, exitMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
fun ModeraBackHandler(
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
}
