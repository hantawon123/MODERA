package com.ssafy.modera.feature.onboarding.impl.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.feature.onboarding.impl.OnboardingPhase
import com.ssafy.modera.feature.onboarding.impl.R
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun BoxWithConstraintsScope.OnboardingFeaturePagerSection(
    phase: OnboardingPhase,
) {
    val screenHeight = maxHeight

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = {
            FEATURE_PAGE_COUNT
        },
    )

    /*
     * 사용자가 넘기지 않으면 일정 시간 후
     * 다음 페이지로 자동 이동.
     *
     * 사용자가 직접 스와이프하는 것도 그대로 허용한다.
     */
    LaunchedEffect(
        phase,
        pagerState.settledPage,
    ) {
        if (phase != OnboardingPhase.FeaturePager) {
            return@LaunchedEffect
        }

        while (true) {
            delay(AUTO_SCROLL_DELAY_MILLIS.milliseconds)

            /*
             * 사용자가 직접 스와이프 중이면
             * 자동 이동하지 않고 다시 기다린다.
             */
            if (pagerState.isScrollInProgress) {
                continue
            }

            val currentPage =
                pagerState.settledPage

            if (currentPage >= LAST_PAGE_INDEX) {
                return@LaunchedEffect
            }

            pagerState.animateScrollToPage(
                page = currentPage + 1,
                animationSpec = tween(
                    durationMillis = PAGE_SCROLL_DURATION_MILLIS,
                ),
            )

            return@LaunchedEffect
        }
    }

    AnimatedVisibility(
        visible = phase == OnboardingPhase.FeaturePager,
        modifier = Modifier.matchParentSize(),
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = SECTION_ENTER_DURATION_MILLIS,
            ),
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            /*
             * 실제 페이지 영역
             *
             * userScrollEnabled를 막지 않았기 때문에
             * 사용자가 직접 좌우로 넘길 수 있다.
             */
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                FeaturePagerPage(
                    page = page,
                    isActive = pagerState.settledPage == page,
                    screenHeight = screenHeight,
                )
            }

            /*
             * Indicator
             */
            PagerIndicator(
                currentPage = pagerState.currentPage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        y = screenHeight * INDICATOR_Y_RATIO,
                    ),
            )

            /*
             * 하단 고정 문구
             */
            Text(
                text = stringResource(
                    R.string.onboarding_feature_notification,
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(
                        y = screenHeight * FOOTER_Y_RATIO,
                    )
                    .padding(
                        horizontal = 24.dp,
                    ),
                style = ModeraTheme.typography.bodyR16,
                color = ModeraTheme.colors.yellow700,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FeaturePagerPage(
    page: Int,
    isActive: Boolean,
    screenHeight: androidx.compose.ui.unit.Dp,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            R.raw.lottie_scanning_screen,
        ),
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isActive,
        restartOnPlay = true,
        iterations = 1,
    )

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        FeaturePageTitle(
            page = page,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    y = screenHeight * TITLE_Y_RATIO,
                )
                .fillMaxWidth()
                .padding(
                    horizontal = 32.dp,
                ),
        )

        LottieAnimation(
            composition = composition,
            progress = {
                progress
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(
                    y = screenHeight * LOTTIE_Y_RATIO,
                )
                .width(LOTTIE_SIZE)
                .aspectRatio(1f),
        )
    }
}

@Composable
private fun FeaturePageTitle(
    page: Int,
    modifier: Modifier = Modifier,
) {
    when (page) {
        CALENDAR_PAGE_INDEX -> {
            val title = buildAnnotatedString {
                withStyle(
                    style = ModeraTheme.typography.titleB22
                        .toSpanStyle()
                        .copy(
                            color = ModeraTheme.colors.gray900,
                        ),
                ) {
                    append(
                        stringResource(
                            R.string.onboarding_feature_calendar_title_prefix,
                        ),
                    )
                }

                withStyle(
                    style = ModeraTheme.typography.titleB22
                        .toSpanStyle()
                        .copy(
                            color = ModeraTheme.colors.yellow700,
                        ),
                ) {
                    append(" ")
                    append(
                        stringResource(
                            R.string.onboarding_feature_calendar_title_highlight,
                        ),
                    )
                    append(" ")
                }

                withStyle(
                    style = ModeraTheme.typography.titleB22
                        .toSpanStyle()
                        .copy(
                            color = ModeraTheme.colors.gray900,
                        ),
                ) {
                    append(
                        stringResource(
                            R.string.onboarding_feature_calendar_title_suffix,
                        ),
                    )
                }
            }

            Text(
                text = title,
                modifier = modifier,
                textAlign = TextAlign.Center,
            )
        }

        SEARCH_PAGE_INDEX -> {
            Text(
                text = stringResource(
                    R.string.onboarding_feature_document_search,
                ),
                modifier = modifier,
                style = ModeraTheme.typography.titleB22,
                color = ModeraTheme.colors.gray900,
                textAlign = TextAlign.Center,
            )
        }

        DOCUMENT_PAGE_INDEX -> {
            Text(
                text = stringResource(
                    R.string.onboarding_feature_document_title,
                ),
                modifier = modifier,
                style = ModeraTheme.typography.titleB22,
                color = ModeraTheme.colors.gray900,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PagerIndicator(
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(FEATURE_PAGE_COUNT) { index ->
            val indicatorColor by animateColorAsState(
                targetValue = if (currentPage == index) {
                    ModeraTheme.colors.yellow700
                } else {
                    ModeraTheme.colors.gray300
                },
                animationSpec = tween(
                    durationMillis = INDICATOR_ANIMATION_DURATION_MILLIS,
                ),
                label = "pagerIndicatorColor",
            )

            Box(
                modifier = Modifier
                    .padding(
                        horizontal = INDICATOR_HORIZONTAL_SPACING,
                    )
                    .size(INDICATOR_SIZE)
                    .background(
                        color = indicatorColor,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

private const val FEATURE_PAGE_COUNT = 3

private const val CALENDAR_PAGE_INDEX = 0
private const val SEARCH_PAGE_INDEX = 1
private const val DOCUMENT_PAGE_INDEX = 2

private const val LAST_PAGE_INDEX =
    FEATURE_PAGE_COUNT - 1

private const val TITLE_Y_RATIO = 0.155f
private const val LOTTIE_Y_RATIO = 0.27f
private const val INDICATOR_Y_RATIO = 0.795f
private const val FOOTER_Y_RATIO = 0.885f

private val LOTTIE_SIZE = 300.dp

private val INDICATOR_SIZE = 10.dp
private val INDICATOR_HORIZONTAL_SPACING = 5.dp

/*
 * 첨부 Lottie가 약 3.4초라
 * 재생 후 약간 머무른 뒤 넘어가도록 4.2초.
 */
private const val AUTO_SCROLL_DELAY_MILLIS = 4_200L

private const val SECTION_ENTER_DURATION_MILLIS = 400
private const val INDICATOR_ANIMATION_DURATION_MILLIS = 220
private const val PAGE_SCROLL_DURATION_MILLIS = 600