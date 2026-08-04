package com.ssafy.modera.core.ui.snackbar

import androidx.annotation.DrawableRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals

data class ModeraSnackbarMessage(
    val message: String,
    @param:DrawableRes val iconRes: Int? = null,
)

data class ModeraSnackbarVisuals(
    override val message: String,
    @param:DrawableRes val iconRes: Int? = null,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) : SnackbarVisuals

fun ModeraSnackbarMessage.toVisuals(): ModeraSnackbarVisuals =
    ModeraSnackbarVisuals(
        message = message,
        iconRes = iconRes,
    )
