package com.ssafy.modera.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import com.ssafy.modera.MainViewModel
import com.ssafy.modera.core.designsystem.component.ModeraNavigationSuiteScaffold
import com.ssafy.modera.core.designsystem.component.Scaffold
import com.ssafy.modera.core.designsystem.component.Text
import com.ssafy.modera.core.designsystem.theme.ModeraTheme
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.core.navigation.toEntries
import com.ssafy.modera.feature.categoryimages.navigation.categoryImagesEntry
import com.ssafy.modera.feature.categoryimages.navigation.navigateToCategoryImages
import com.ssafy.modera.feature.home.HomeAnalysisState
import com.ssafy.modera.feature.home.LocalHomeAnalysisState
import com.ssafy.modera.feature.home.navigation.HomeNavKey
import com.ssafy.modera.feature.home.navigation.homeEntry
import com.ssafy.modera.feature.categoryimages.navigation.categoryImagesEntry
import com.ssafy.modera.feature.imagedetail.navigation.imageDetailEntry
import com.ssafy.modera.media.rememberGalleryPickerLauncher
import com.ssafy.modera.navigation.RegisterNavKey
import com.ssafy.modera.navigation.SearchNavKey
import com.ssafy.modera.navigation.TOP_LEVEL_NAV_ITEMS

@Composable
fun ModeraApp(
    appState: ModeraAppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
    viewModel: MainViewModel = hiltViewModel(),
) {
//    val shouldShowGradientBackground = appState.navigationState.currentTopLevelKey == ForYouNavKey
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val localSnackbarHostState = compositionLocalOf<SnackbarHostState> {
        error("SnackbarHostState state should be initialized at runtime")
    }

    CompositionLocalProvider(localSnackbarHostState provides snackbarHostState) {
        ModeraApp(
            appState = appState,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            windowAdaptiveInfo = windowAdaptiveInfo,
            modifier = modifier,
        )
    }
}

@Composable
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
internal fun ModeraApp(
    appState: ModeraAppState,
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = remember { Navigator(appState.navigationState) }
    val homeAnalysisState = remember(viewModel) {
        HomeAnalysisState(onDismissRequest = viewModel::dismissAnalysisBanner)
    }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message = message)
        }
    }

    LaunchedEffect(uiState.showAnalysisBanner, uiState.analysisImageCount) {
        homeAnalysisState.sync(
            showBanner = uiState.showAnalysisBanner,
            imageCount = uiState.analysisImageCount,
        )
    }

    val launchGalleryPicker = rememberGalleryPickerLauncher(
        onImagesPicked = { images ->
            if (images.isEmpty()) return@rememberGalleryPickerLauncher
            viewModel.onImagesPicked(images)
            navigator.navigate(HomeNavKey)
        },
    )

    val isTopLevelDestination =
        appState.navigationState.currentKey == appState.navigationState.currentTopLevelKey

    CompositionLocalProvider(
        LocalHomeAnalysisState provides homeAnalysisState,
    ) {
        ModeraNavigationSuiteScaffold(
            layoutType = if (isTopLevelDestination) {
                null
            } else {
                NavigationSuiteType.None
            },
            navigationSuiteItems = {
                if (isTopLevelDestination) {
                    TOP_LEVEL_NAV_ITEMS.forEach { (navKey, navItem) ->
                        val selected = navKey != RegisterNavKey &&
                            navKey == appState.navigationState.currentTopLevelKey

                        item(
                            selected = selected,
                            onClick = {
                                if (navKey == RegisterNavKey) {
                                    launchGalleryPicker()
                                } else {
                                    navigator.navigate(navKey)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(navItem.unselectedIcon),
                                    contentDescription = null,
                                )
                            },
                            selectedIcon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(navItem.selectedIcon),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(navItem.iconTextId)) },
                            modifier = Modifier,
                        )
                    }
                }
            },
            windowAdaptiveInfo = windowAdaptiveInfo,
        ) {
            Scaffold(
                modifier = modifier,
                containerColor = Color.Transparent,
                contentColor = Color.Gray, // TODO : 추후 컬러 변경
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.exclude(
                            WindowInsets.ime,
                        ),
                    ),
                )
                },
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal,
                            ),
                        ),
                ) {
                    // Only show the top app bar on top level destinations.
                    var shouldShowTopAppBar = false

                    if (appState.navigationState.currentKey in appState.navigationState.topLevelKeys) {
                        shouldShowTopAppBar = true

                        val destination =
                            TOP_LEVEL_NAV_ITEMS[appState.navigationState.currentTopLevelKey]
                                ?: error("Top level nav item not found for ${appState.navigationState.currentTopLevelKey}")
                    }

                    Box(
                        modifier = Modifier.consumeWindowInsets(
                            if (shouldShowTopAppBar) {
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                            } else {
                                WindowInsets(0, 0, 0, 0)
                            },
                        ),
                    ) {
                        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

                        val entryProvider = entryProvider {
                            homeEntry(
                                navigator = navigator,
                                onCategoryClick = navigator::navigateToCategoryImages,
                            )
                            categoryImagesEntry(navigator)
                            imageDetailEntry(navigator)
                            registerEntry(navigator)
                            searchEntry(navigator)
                            categoryImagesEntry(navigator)
                        }

                        NavDisplay(
                            entries = appState.navigationState.toEntries(entryProvider),
//                        sceneStrategy = listDetailStrategy,
                            onBack = { navigator.goBack() },
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.notificationDot(): Modifier =
    composed {
        val tertiaryColor = ModeraTheme.colors.blue
        drawWithContent {
            drawContent()
            drawCircle(
                tertiaryColor,
                radius = 5.dp.toPx(),
                center = center + Offset(
                    64.dp.toPx() * .45f,
                    32.dp.toPx() * -.45f - 6.dp.toPx(),
                ),
            )
        }
    }

// TODO : 추후 각 NavigationProvider에 추가
fun EntryProviderScope<NavKey>.registerEntry(navigator: Navigator) {
    entry<RegisterNavKey> {}
}

fun EntryProviderScope<NavKey>.searchEntry(navigator: Navigator) {
    entry<SearchNavKey> {}
}
