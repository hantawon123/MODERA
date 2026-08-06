package com.ssafy.modera

import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSystemSplash.get() }
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            splashScreenViewProvider.remove()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isSplashFinished by rememberSaveable {
                mutableStateOf(false)
            }

            val notificationTarget by
            pendingNotificationTarget.collectAsStateWithLifecycle()

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
                        notificationNavigationTarget =
                            notificationTarget,
                        onNotificationNavigationConsumed = {
                            pendingNotificationTarget.value = null
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
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(
        intent: Intent?,
    ) {
        val target = intent
            ?.toNotificationNavigationTarget()
            ?: return

        pendingNotificationTarget.value = target
    }
}
