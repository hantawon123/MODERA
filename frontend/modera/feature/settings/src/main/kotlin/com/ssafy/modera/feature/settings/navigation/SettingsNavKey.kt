package com.ssafy.modera.feature.settings.navigation

import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import kotlinx.serialization.Serializable

@Serializable
object SettingsNavKey : NavKey

fun Navigator.navigateToSettings() {
    navigate(SettingsNavKey)
}
