package com.ssafy.modera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.splash.SplashScreen
import com.ssafy.modera.ui.ModeraApp
import com.ssafy.modera.ui.rememberModeraAppState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var keepOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepOnScreen }

        setContent {
            var isSplashFinished by rememberSaveable {
                mutableStateOf(false)
            }

            SideEffect {
                keepOnScreen = false
            }

            ModeraTheme {
                if (isSplashFinished) {
                    val appState = rememberModeraAppState()

                    ModeraApp(appState)
                } else {
                    SplashScreen(
                        onFinished = { isSplashFinished = true },
                    )
                }
            }
        }
    }
}
