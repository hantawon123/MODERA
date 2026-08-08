package com.ssafy.modera.feature.settings.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme

@Composable
fun SettingsMenuItem(
    @DrawableRes iconRes: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    statusText: String? = null,
    statusColor: Color = ModeraTheme.colors.gray500,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = ModeraTheme.colors.gray700,
        )

        Spacer(
            modifier = Modifier.width(16.dp),
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = ModeraTheme.typography.bodyR16,
                color = ModeraTheme.colors.gray900,
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 4.dp),
                    style = ModeraTheme.typography.bodyR14,
                    color = ModeraTheme.colors.gray400,
                )
            }
        }

        if (statusText != null) {
            Spacer(
                modifier = Modifier.width(12.dp),
            )

            Text(
                text = statusText,
                style = ModeraTheme.typography.bodyR14,
                color = statusColor,
            )
        }

        Spacer(
            modifier = Modifier.width(8.dp),
        )

        Icon(
            imageVector = ImageVector.vectorResource(ModeraIcons.ArrowRight),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = ModeraTheme.colors.gray400,
        )
    }
}