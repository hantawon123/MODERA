package com.ssafy.modera.feature.onboading.impl.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.onboading.impl.R

@Composable
internal fun BoxWithConstraintsScope.OnboardingSkipButton(
    onClick: () -> Unit,
) {
    Text(
        text = stringResource(
            R.string.onboarding_skip,
        ),
        modifier = Modifier
            .align(Alignment.TopEnd)
            .statusBarsPadding()
            .padding(
                top = 8.dp,
                end = 12.dp,
            )
            .clickable(
                onClick = onClick,
            )
            .padding(
                horizontal = 8.dp,
                vertical = 6.dp,
            ),
        style = ModeraTheme.typography.bodyR16,
        color = ModeraTheme.colors.gray400,
    )
}