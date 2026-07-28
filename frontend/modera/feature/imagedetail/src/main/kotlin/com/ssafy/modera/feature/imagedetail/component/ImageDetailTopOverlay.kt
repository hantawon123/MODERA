package com.ssafy.modera.feature.imagedetail.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.R as designSystemR
import com.ssafy.modera.core.designsystem.component.ClickableSurface
import com.ssafy.modera.core.designsystem.component.Icon
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.imagedetail.R

@Composable
internal fun ImageDetailTopOverlay(
    visible: Boolean,
    onBackClick: () -> Unit,
    onReanalyzeClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .statusBarsPadding()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClickableSurface(
                onClick = onBackClick,
                color = Color.Transparent,
            ) {
                Icon(
                    painter = painterResource(designSystemR.drawable.ic_arrow_left_24),
                    contentDescription = stringResource(R.string.image_detail_back),
                    modifier = Modifier.size(24.dp),
                    tint = Color.White,
                )
            }

            Box(modifier = Modifier.weight(1f))

            ClickableSurface(
                onClick = onReanalyzeClick,
                color = Color.Transparent,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.image_detail_reanalyze_action),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp),
                    tint = Color.White,
                )
            }

            ClickableSurface(
                onClick = onMoreClick,
                color = Color.Transparent,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vertical),
                    contentDescription = stringResource(R.string.image_detail_more_menu),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp),
                    tint = Color.White,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImageDetailTopOverlayPreview() {
    ModeraTheme {
        ImageDetailTopOverlay(
            visible = true,
            onBackClick = {},
            onReanalyzeClick = {},
            onMoreClick = {},
        )
    }
}
