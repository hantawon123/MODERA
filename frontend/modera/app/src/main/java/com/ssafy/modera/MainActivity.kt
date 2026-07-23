package com.ssafy.modera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.ui.ModeraApp
import com.ssafy.modera.ui.rememberModeraAppState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appState = rememberModeraAppState()

            ModeraTheme {
                ModeraApp(appState)
            }
        }
    }
}
