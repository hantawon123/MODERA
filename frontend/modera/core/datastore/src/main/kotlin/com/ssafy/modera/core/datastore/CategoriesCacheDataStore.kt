package com.ssafy.modera.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.ssafy.modera.core.datastore.proto.CategoriesCache

internal val Context.categoriesCacheDataStore: DataStore<CategoriesCache> by dataStore(
    fileName = "categories_cache.pb",
    serializer = CategoriesCacheSerializer,
)
