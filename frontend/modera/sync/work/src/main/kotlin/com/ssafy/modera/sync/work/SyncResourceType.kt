package com.ssafy.modera.sync.work

import com.ssafy.modera.core.common.sync.SyncCompletedResource

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

internal fun SyncResourceType.toCompletedResource():
        SyncCompletedResource =
    when (this) {
        SyncResourceType.IMAGE_UPLOAD -> {
            SyncCompletedResource.IMAGE_UPLOAD
        }

        SyncResourceType.IMAGE_CATEGORY -> {
            SyncCompletedResource.IMAGE_CATEGORY
        }

        SyncResourceType.DOCUMENT_UPLOAD -> {
            SyncCompletedResource.DOCUMENT_UPLOAD
        }

        SyncResourceType.DOCUMENT_REANALYSIS -> {
            SyncCompletedResource.DOCUMENT_REANALYSIS
        }

        SyncResourceType.CALENDAR -> {
            SyncCompletedResource.CALENDAR
        }
    }