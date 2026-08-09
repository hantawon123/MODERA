package com.ssafy.modera.core.data.mapper

import com.ssafy.modera.core.datastore.proto.CategoryProto
import com.ssafy.modera.core.model.category.Category

internal fun CategoryProto.toDomainModel(): Category =
    Category(
        id = id,
        title = title,
        thumbnailUrl = thumbnailUrl.takeIf(String::isNotEmpty),
        itemCount = itemCount,
        tags = emptyList(),
        isNew = isNew,
    )

internal fun Category.toProtoModel(): CategoryProto =
    CategoryProto.newBuilder()
        .setId(id)
        .setTitle(title)
        .setThumbnailUrl(thumbnailUrl.orEmpty())
        .setItemCount(itemCount)
        .setIsNew(isNew)
        .build()
