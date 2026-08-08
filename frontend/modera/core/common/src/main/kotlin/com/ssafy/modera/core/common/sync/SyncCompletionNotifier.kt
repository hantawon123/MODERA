package com.ssafy.modera.core.common.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncCompletionNotifier @Inject constructor() {

    val events: SharedFlow<SyncCompletedEvent>
        field = MutableSharedFlow<SyncCompletedEvent>(
            extraBufferCapacity = EVENT_BUFFER_CAPACITY,
        )

    suspend fun notifyCompleted(
        event: SyncCompletedEvent,
    ) {
        events.emit(event)
    }

    private companion object {
        const val EVENT_BUFFER_CAPACITY = 8
    }
}