package com.ssafy.modera.feature.documentcreate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.util.moderaShimmer

@Composable
internal fun DocumentRecommendationSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        repeat(RecommendationSkeletonCount) {
            DocumentRecommendationSkeletonItem()
        }
    }
}

@Composable
private fun DocumentRecommendationSkeletonItem(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(18.dp)
                    .moderaShimmer(
                        shape = RoundedCornerShape(4.dp),
                    ),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .moderaShimmer(
                        shape = RoundedCornerShape(4.dp),
                    ),
            )

            Spacer(modifier = Modifier.height(6.dp))

            Spacer(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp)
                    .moderaShimmer(
                        shape = RoundedCornerShape(4.dp),
                    ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(
                    modifier = Modifier
                        .width(36.dp)
                        .height(14.dp)
                        .moderaShimmer(
                            shape = RoundedCornerShape(4.dp),
                        ),
                )

                Spacer(
                    modifier = Modifier
                        .width(44.dp)
                        .height(14.dp)
                        .moderaShimmer(
                            shape = RoundedCornerShape(4.dp),
                        ),
                )

                Spacer(
                    modifier = Modifier
                        .width(32.dp)
                        .height(14.dp)
                        .moderaShimmer(
                            shape = RoundedCornerShape(4.dp),
                        ),
                )
            }
        }

        Spacer(
            modifier = Modifier
                .size(88.dp)
                .moderaShimmer(
                    shape = RoundedCornerShape(8.dp),
                ),
        )
    }
}

private const val RecommendationSkeletonCount = 4

@Preview(
    name = "Document Recommendation Loading",
    showBackground = true,
    widthDp = 360,
)
@Composable
private fun DocumentRecommendationLoadingScreenPreview() {
    ModeraTheme {
        DocumentRecommendationSkeleton(
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}