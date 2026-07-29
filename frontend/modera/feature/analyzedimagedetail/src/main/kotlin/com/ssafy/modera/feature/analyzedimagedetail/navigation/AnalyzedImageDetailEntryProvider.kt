package com.ssafy.modera.feature.analyzedimagedetail.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.ssafy.modera.core.navigation.Navigator
import com.ssafy.modera.feature.analyzedimagedetail.AnalyzedImageDetailScreen
import com.ssafy.modera.feature.analyzedimagedetail.AnalyzedImageDetailViewModel
import com.ssafy.modera.feature.analyzedimagedetail.AnalyzedImageDetailViewModel.Factory

fun EntryProviderScope<NavKey>.analyzedImageDetailEntry(
    navigator: Navigator,
) {
    entry<AnalyzedImageDetailNavKey> { key ->
        val id = key.imageId

//        AnalyzedImageDetailScreen(
//            onBackClick = navigator::goBack,
//            onCategoryClick = { /* TODO: 카테고리 검색 화면으로 이동 */ },
//            onReanalyzeClick = { /* TODO: 이미지 재분석 API 연동 */ },
//            onRelatedMaterialsClick = { /* TODO: 연관 자료 리스트 화면으로 이동 */ },
//            onCopyOcrTextClick = { /* TODO: 클립보드 복사 */ },
//            onViewImageInfoClick = { /* TODO: 이미지 정보 다이얼로그 */ },
//            onDeleteClick = { /* TODO: 이미지 삭제 API 연동 */ },
//            onFavoriteClick = { /* TODO: 즐겨찾기 토글 */ },
//            viewModel = hiltViewModel<AnalyzedImageDetailViewModel, Factory>(
//                key = id.toString(),
//            ) { factory ->
//                factory.create(id)
//            },
//        )
    }
}
