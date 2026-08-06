package com.ssafy.modera.feature.analyzedimage.related.images.component

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
internal fun RelatedImagesHeader(
    sourceTitle: String,
    relatedImageCount: Int,
    modifier: Modifier = Modifier,
) {
    val formattedSourceTitle = stringResource(
        R.string.related_images_source_title_format,
        sourceTitle,
    )
    val headerSuffix = stringResource(
        R.string.related_images_header_suffix,
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
private fun RelatedImagesHeaderPreview() {
    ModeraTheme {
        RelatedImagesHeader(
            sourceTitle = "ASCII 해커톤",
            relatedImageCount = 3,
            modifier = Modifier.padding(20.dp)
        )
    }
}