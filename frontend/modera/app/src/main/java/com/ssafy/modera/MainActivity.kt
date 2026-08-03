package com.ssafy.modera

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
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.ui.ModeraApp
import com.ssafy.modera.ui.rememberModeraAppState
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean
import com.ssafy.modera.feature.splash.SplashScreen as ModeraSplashScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val keepSystemSplash = AtomicBoolean(true)

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
                    ModeraApp(appState)
                } else {
                    ModeraSplashScreen(
                        onFinished = { isSplashFinished = true },
                    )
                }
            }
        }
    }
}
