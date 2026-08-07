package com.ssafy.modera.feature.analyzedimage.related.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.analyzedimage.related.R

@Composable
internal fun RelatedHeader(
    sourceTitle: String,
    relatedImageCount: Int,
    headerSuffix: String,
    modifier: Modifier = Modifier,
) {
    val formattedSourceTitle = stringResource(
        R.string.related_source_title_format,
        sourceTitle,
    )

    val headerText = buildAnnotatedString {
        withStyle(
            style = ModeraTheme.typography.bodySB16
                .toSpanStyle()
                .copy(
                    color = ModeraTheme.colors.gray900,
                ),
        ) {
            append(formattedSourceTitle)
        }

        withStyle(
            style = ModeraTheme.typography.bodyR16
                .toSpanStyle()
                .copy(
                    color = ModeraTheme.colors.gray700,
                ),
        ) {
            append(headerSuffix)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = headerText,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = relatedImageCount.toString(),
            style = ModeraTheme.typography.captionSB12,
            color = ModeraTheme.colors.yellow800,
            modifier = Modifier
                .clip(CircleShape)
                .background(ModeraTheme.colors.yellow700Bg)
                .padding(
                    horizontal = 8.dp,
                    vertical = 2.dp
                ),
        )
    }
}

@Preview(name = "Related Images Header", showBackground = true)
@Composable
private fun RelatedHeaderPreview() {
    ModeraTheme {
        RelatedHeader(
            sourceTitle = "ASCII 해커톤",
            relatedImageCount = 3,
            headerSuffix = "와 관련된 자료",
            modifier = Modifier.padding(20.dp)
        )
    }
}