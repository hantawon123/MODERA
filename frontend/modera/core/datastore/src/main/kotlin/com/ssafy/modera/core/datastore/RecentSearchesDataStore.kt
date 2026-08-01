package com.ssafy.modera.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.ssafy.modera.core.datastore.proto.RecentSearches

internal val Context.recentSearchesDataStore: DataStore<RecentSearches> by dataStore(
    fileName = "recent_searches.pb",
    serializer = RecentSearchesSerializer,
)
