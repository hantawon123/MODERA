package com.ssafy.modera.fcm

import com.ssafy.modera.sync.work.SyncResourceType

data class DataChangedEvent(
    val eventId: String,
    val resource: SyncResourceType,
    val resourceId: Long,
    val occurredAt: String,
)

internal fun Map<String, String>.toDataChangedEvent(): DataChangedEvent? {
    if (this[KEY_TYPE] != DATA_CHANGED) {
        return null
    }

    val eventId = this[KEY_EVENT_ID]
        ?.takeIf(String::isNotBlank)
        ?: return null

    val resource = SyncResourceType.fromServerValue(
        value = this[KEY_RESOURCE],
    ) ?: return null

    val resourceId = this[KEY_RESOURCE_ID]
        ?.toLongOrNull()
        ?.takeIf { it > 0L }
        ?: return null

    val occurredAt = this[KEY_OCCURRED_AT]
        ?.takeIf(String::isNotBlank)
        ?: return null

    return DataChangedEvent(
        eventId = eventId,
        resource = resource,
        resourceId = resourceId,
        occurredAt = occurredAt,
    )
}

private const val DATA_CHANGED = "DATA_CHANGED"

private const val KEY_TYPE = "type"
private const val KEY_EVENT_ID = "eventId"
private const val KEY_RESOURCE = "resource"
private const val KEY_RESOURCE_ID = "resourceId"
private const val KEY_OCCURRED_AT = "occurredAt"