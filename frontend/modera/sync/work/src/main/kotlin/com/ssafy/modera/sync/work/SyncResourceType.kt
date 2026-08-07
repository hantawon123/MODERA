package com.ssafy.modera.sync.work

enum class SyncResourceType {
    IMAGE_UPLOAD,
    IMAGE_CATEGORY,
    DOCUMENT_UPLOAD,
    DOCUMENT_REANALYSIS,
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