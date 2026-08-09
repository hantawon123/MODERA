package com.ssafy.modera.feature.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.icon.ModeraIcons
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.home.R

@Composable
internal fun HomeUpperSection(
    upperContentAlpha: Float,
    onCalendarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = upperContentAlpha },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(ModeraIcons.CalendarNumber),
                contentDescription = stringResource(R.string.home_calendar_content_description),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onCalendarClick),
                tint = ModeraTheme.colors.gray700,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = ImageVector.vectorResource(ModeraIcons.Settings),
                contentDescription = stringResource(R.string.home_settings_content_description),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onSettingsClick),
                tint = ModeraTheme.colors.gray700,
            )
        }

        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            BasicText(
                text = stringResource(R.string.home_hero_title),
                style = ModeraTheme.typography.titleB20.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            ModeraTheme.colors.yellow500,
                            ModeraTheme.colors.yellow700,
                        ),
                    ),
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_hero_subtitle),
                style = ModeraTheme.typography.captionR12,
                color = ModeraTheme.colors.gray400,
            )
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.img_search_character),
                contentDescription = stringResource(R.string.home_hero_character_content_description),
                modifier = Modifier
                    .size(HomeHeroDefaults.CharacterSize)
                    .align(Alignment.End),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private object HomeHeroDefaults {
    val CharacterSize = 96.dp
}

@Preview(
    name = "Home Upper Section",
    showBackground = true,
    widthDp = 360,
    heightDp = 480,
)
@Composable
private fun HomeUpperSectionPreview() {
    ModeraTheme {
        HomeUpperSection(
            upperContentAlpha = 1f,
            onCalendarClick = {},
            onSettingsClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .padding(horizontal = 20.dp),
        )
    }
}

@Preview(
    name = "Home Upper Section - Search Active",
    showBackground = true,
    widthDp = 360,
    heightDp = 120,
)
@Composable
private fun HomeUpperSectionSearchActivePreview() {
    ModeraTheme {
        HomeUpperSection(
            upperContentAlpha = 0f,
            onCalendarClick = {},
            onSettingsClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 20.dp),
        )
    }
}

