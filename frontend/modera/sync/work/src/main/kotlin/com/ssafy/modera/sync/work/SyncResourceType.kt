package com.ssafy.modera.sync.work

enum class SyncResourceType {
    IMAGE,
    IMAGE_CATEGORY,
    DOCUMENT,
    CALENDAR,
    ;

    companion object {
        fun fromServerValue(
            value: String?,
        ): SyncResourceType? =
            entries.firstOrNull { resourceType ->
                resourceType.name.equals(
                    other = value,
                    ignoreCase = true,
                )
            }
    }
}