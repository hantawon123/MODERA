package com.ssafy.modera.feature.document

import androidx.annotation.StringRes

enum class DocumentSortType(
    @get:StringRes val labelRes: Int,
) {
    LATEST(
        labelRes = R.string.document_sort_latest,
    ),
    OLDEST(
        labelRes = R.string.document_sort_oldest,
    ),
}