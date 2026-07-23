package com.ssafy.modera.feature.imagedetail.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ImageDetailNavKey(
    val imageId: Long,
) : NavKey
