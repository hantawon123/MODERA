package com.ssafy.modera.feature.category

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryTabController @Inject constructor() {
    private val _showAllRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val showAllRequests: SharedFlow<Unit> = _showAllRequests.asSharedFlow()

    fun showAll() {
        _showAllRequests.tryEmit(Unit)
    }

    fun observeShowAll(
        scope: CoroutineScope,
        onShowAll: () -> Unit,
    ) {
        scope.launch {
            showAllRequests.collect {
                onShowAll()
            }
        }
    }
}
