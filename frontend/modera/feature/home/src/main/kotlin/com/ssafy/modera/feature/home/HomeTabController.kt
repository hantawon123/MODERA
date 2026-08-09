package com.ssafy.modera.feature.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeTabController @Inject constructor() {
    private val _resetRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val resetRequests: SharedFlow<Unit> = _resetRequests.asSharedFlow()

    fun resetToDefault() {
        _resetRequests.tryEmit(Unit)
    }

    fun observeReset(
        scope: CoroutineScope,
        onReset: () -> Unit,
    ) {
        scope.launch {
            resetRequests.collect {
                onReset()
            }
        }
    }
}
