package com.ssafy.modera.core.common.sync

data class SyncCompletedEvent(
    val resource: SyncCompletedResource,
    val resourceId: Long,
)