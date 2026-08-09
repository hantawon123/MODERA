package com.ssafy.modera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.fcm.NotificationNavigationTarget
import com.ssafy.modera.fcm.toNotificationNavigationTarget
import com.ssafy.modera.media.extractSharedImageUris
import com.ssafy.modera.ui.ModeraApp
import com.ssafy.modera.ui.rememberModeraAppState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import com.ssafy.modera.feature.splash.SplashScreen as ModeraSplashScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val keepSystemSplash = AtomicBoolean(true)
    private val pendingNotificationTarget =
        MutableStateFlow<NotificationNavigationTarget?>(null)
    private val pendingSharedImageUris =
        MutableStateFlow<List<Uri>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSystemSplash.get() }
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.remove()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            var isSplashFinished by rememberSaveable {
                mutableStateOf(false)
            }

            val notificationTarget by
                pendingNotificationTarget.collectAsStateWithLifecycle()
            val sharedImageUris by
                pendingSharedImageUris.collectAsStateWithLifecycle()

            val view = LocalView.current

            DisposableEffect(view) {
                val preDrawListener = view.doOnPreDraw {
                    keepSystemSplash.set(false)
                }
                onDispose {
                    preDrawListener.removeListener()
                }
            }

            ModeraTheme {
                if (isSplashFinished) {
                    val appState = rememberModeraAppState()
                    ModeraApp(
                        appState = appState,
                        notificationNavigationTarget = notificationTarget,
                        onNotificationNavigationConsumed = {
                            pendingNotificationTarget.value = null
                        },
                        sharedImageUris = sharedImageUris,
                        onSharedImagesConsumed = {
                            pendingSharedImageUris.value = null
                            clearShareIntentExtras()
                        },
                    )
                } else {
                    ModeraSplashScreen(
                        onFinished = { isSplashFinished = true },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(
        intent: Intent?,
    ) {
        handleNotificationIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleNotificationIntent(
        intent: Intent?,
    ) {
        val target = intent
            ?.toNotificationNavigationTarget()
            ?: return

        pendingNotificationTarget.value = target
    }

    private fun handleShareIntent(
        intent: Intent?,
    ) {
        val uris = intent
            ?.extractSharedImageUris()
            .orEmpty()

        if (uris.isEmpty()) return

        pendingSharedImageUris.value = uris
    }

    private fun clearShareIntentExtras() {
        val currentIntent = intent ?: return
        if (
            currentIntent.action != Intent.ACTION_SEND &&
            currentIntent.action != Intent.ACTION_SEND_MULTIPLE
        ) {
            return
        }

        currentIntent.removeExtra(Intent.EXTRA_STREAM)
        currentIntent.action = Intent.ACTION_MAIN
        currentIntent.type = null
    }
}
